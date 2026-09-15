import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { messageErreur } from '../../../core/services/api-error';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../shared/models/books';
import { Users } from '../../../shared/models/users';
import { Reservation, ReservationRequest, ReservationStatus } from '../../../shared/models/reservation';
import { ReservationService } from '../../../core/api/reservation.service';
import { BooksService } from '../../../core/api/books.service';
import { UsersService } from '../../../core/api/users.service';
import { UserAuthService } from '../../../core/services/user-auth.service';
import { TranslationService } from '../../../core/services/translation.service';

type EtatEcran = 'chargement' | 'donnees' | 'vide' | 'erreur';
type Onglet = 'toutes' | 'expirees';

const STATUTS: ReservationStatus[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

@Component({
  selector: 'app-reservations',
  templateUrl: './reservations.component.html',
  styleUrls: ['./reservations.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReservationsComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  readonly statuts = STATUTS;

  etat: EtatEcran = 'chargement';
  reservations: Reservation[] = [];
  filtreStatut: ReservationStatus | '' = '';
  onglet: Onglet = 'toutes';

  livres: Books[] = [];
  adherents: Users[] = [];

  creationEnCours = false;
  erreurFormulaire: string | null = null;
  resetFormulaire = 0;

  annulationEnCoursId: number | null = null;
  erreurAnnulation: string | null = null;

  suppressionEnCoursId: number | null = null;
  erreurSuppression: string | null = null;

  reservationsExpirees: Reservation[] = [];
  etatExpirees: EtatEcran = 'chargement';

  constructor(
    private reservationService: ReservationService,
    private booksService: BooksService,
    private usersService: UsersService,
    private userAuthService: UserAuthService,
    private translationService: TranslationService,
    private cdr: ChangeDetectorRef
  ) { }

  /**
   * Un BIBLIOTHECAIRE peut créer une réservation pour n'importe quel
   * adhérent ; un ADHERENT uniquement pour lui-même (RS-04).
   */
  get estBibliothecaire(): boolean {
    const roles = this.userAuthService.getRoles() || [];
    return roles.some((r) => r.roleName === 'BIBLIOTHECAIRE');
  }

  get monUserId(): number | null {
    try {
      const userId = this.userAuthService.getUserId();
      return typeof userId === 'number' ? userId : null;
    } catch {
      return null;
    }
  }

  ngOnInit(): void {
    this.chargerReservations();
    this.chargerReferentiels();
    if (this.estBibliothecaire) {
      this.chargerExpirees();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  chargerReservations(): void {
    this.etat = 'chargement';
    const statut = this.filtreStatut || undefined;
    this.reservationService.getReservations(statut as ReservationStatus | undefined).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'vide' : 'donnees';
        this.rafraichir();
      },
      error: () => {
        this.etat = 'erreur';
        this.rafraichir();
      }
    });
  }

  private chargerReferentiels(): void {
    this.booksService.getBooksList().pipe(takeUntil(this.destroy$)).subscribe({
      // Le dropdown liste tous les livres, disponibles inclus : c'est ce qui
      // permet de déclencher volontairement le 409 RG-01 (voir passage devant
      // le formateur, séance 3).
      next: (livres) => {
        this.livres = livres || [];
        this.rafraichir();
      },
      error: () => {
        this.livres = [];
        this.rafraichir();
      }
    });

    // Seul un BIBLIOTHECAIRE choisit l'adhérent ; un ADHERENT réserve
    // pour lui-même (RS-04).
    if (!this.estBibliothecaire) {
      this.adherents = [];
      return;
    }

    this.usersService.getUsersList().pipe(takeUntil(this.destroy$)).subscribe({
      next: (users) => {
        this.adherents = (users || []).filter(
          (u) => u.role && u.role.some((r) => r.roleName === 'User'));
        this.rafraichir();
      },
      error: () => {
        this.adherents = [];
        this.rafraichir();
      }
    });
  }

  onFiltreChange(): void {
    this.chargerReservations();
  }

  onOngletChange(onglet: Onglet): void {
    this.onglet = onglet;
  }

  onCreer(request: ReservationRequest): void {
    this.creationEnCours = true;
    this.erreurFormulaire = null;

    this.reservationService.createReservation(request).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.creationEnCours = false;
        this.resetFormulaire++;
        this.chargerReservations();
        this.rafraichir();
      },
      error: (err: HttpErrorResponse) => {
        this.creationEnCours = false;
        this.erreurFormulaire = this.messageErreur(err, 'La création de la réservation a échoué.');
        this.rafraichir();
      }
    });
  }

  onAnnuler(id: number): void {
    this.annulationEnCoursId = id;
    this.erreurAnnulation = null;
    this.reservationService.annulerReservation(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.annulationEnCoursId = null;
        this.chargerReservations();
        this.rafraichir();
      },
      error: (err: HttpErrorResponse) => {
        this.annulationEnCoursId = null;
        this.erreurAnnulation = this.messageErreur(err, "L'annulation a échoué.");
        this.rafraichir();
      }
    });
  }

  onSupprimer(id: number): void {
    const reservation = this.reservations.find(r => r.id === id)
      || this.reservationsExpirees.find(r => r.id === id);
    const nom = reservation?.livreNom || `#${id}`;
    const confirme = window.confirm(
      this.translationService.translate('reservations.confirmDelete', { book: nom }));
    if (!confirme) { return; }

    this.suppressionEnCoursId = id;
    this.erreurSuppression = null;
    this.reservationService.deleteReservation(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.suppressionEnCoursId = null;
        this.chargerReservations();
        this.chargerExpirees();
        this.rafraichir();
      },
      error: (err: HttpErrorResponse) => {
        this.suppressionEnCoursId = null;
        this.erreurSuppression = this.messageErreur(err, "La suppression a échoué.");
        this.rafraichir();
      }
    });
  }

  chargerExpirees(): void {
    this.etatExpirees = 'chargement';
    this.reservationService.getExpiredReservations().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.reservationsExpirees = data;
        this.etatExpirees = data.length === 0 ? 'vide' : 'donnees';
        this.rafraichir();
      },
      error: () => {
        this.etatExpirees = 'erreur';
        this.rafraichir();
      }
    });
  }

  /** OnPush : une réponse HTTP ne marque pas la vue comme modifiée. */
  private rafraichir(): void {
    this.cdr.markForCheck();
  }

  private messageErreur(err: HttpErrorResponse, repli: string): string {
    return messageErreur(err, repli);
  }
}
