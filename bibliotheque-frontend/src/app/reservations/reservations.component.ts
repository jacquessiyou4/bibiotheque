import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { Reservation, ReservationRequest, StatutReservation } from '../_model/reservation';
import { ReservationService } from '../_service/reservation.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { UserAuthService } from '../_service/user-auth.service';
import { TranslationService } from '../_service/translation.service';

type EtatEcran = 'chargement' | 'donnees' | 'vide' | 'erreur';
type Onglet = 'toutes' | 'expirees';

const STATUTS: StatutReservation[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

@Component({
  selector: 'app-reservations',
  templateUrl: './reservations.component.html',
  styleUrls: ['./reservations.component.css']
})
export class ReservationsComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  readonly statuts = STATUTS;

  etat: EtatEcran = 'chargement';
  reservations: Reservation[] = [];
  filtreStatut: StatutReservation | '' = '';
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
    private translationService: TranslationService
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
    this.reservationService.getReservations(statut as StatutReservation | undefined).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'vide' : 'donnees';
      },
      error: () => {
        this.etat = 'erreur';
      }
    });
  }

  private chargerReferentiels(): void {
    this.booksService.getBooksList().pipe(takeUntil(this.destroy$)).subscribe({
      // Le dropdown liste tous les livres, disponibles inclus : c'est ce qui
      // permet de déclencher volontairement le 409 RG-01 (voir passage devant
      // le formateur, séance 3).
      next: (livres) => this.livres = livres || [],
      error: () => this.livres = []
    });

    // Seul un BIBLIOTHECAIRE choisit l'adhérent ; un ADHERENT réserve
    // pour lui-même (RS-04).
    if (!this.estBibliothecaire) {
      this.adherents = [];
      return;
    }

    this.usersService.getUsersList().pipe(takeUntil(this.destroy$)).subscribe({
      next: (users) => this.adherents = (users || []).filter(
        (u) => u.role && u.role.some((r) => r.roleName === 'User')),
      error: () => this.adherents = []
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
      },
      error: (err: HttpErrorResponse) => {
        this.creationEnCours = false;
        this.erreurFormulaire = this.messageErreur(err, 'La création de la réservation a échoué.');
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
      },
      error: (err: HttpErrorResponse) => {
        this.annulationEnCoursId = null;
        this.erreurAnnulation = this.messageErreur(err, "L'annulation a échoué.");
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
      },
      error: (err: HttpErrorResponse) => {
        this.suppressionEnCoursId = null;
        this.erreurSuppression = this.messageErreur(err, "La suppression a échoué.");
      }
    });
  }

  chargerExpirees(): void {
    this.etatExpirees = 'chargement';
    this.reservationService.getExpiredReservations().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.reservationsExpirees = data;
        this.etatExpirees = data.length === 0 ? 'vide' : 'donnees';
      },
      error: () => {
        this.etatExpirees = 'erreur';
      }
    });
  }

  private messageErreur(err: HttpErrorResponse, repli: string): string {
    if (err.status === 0) {
      return "Le serveur est injoignable. Vérifiez qu'il est démarré, puis réessayez.";
    }
    if (err.error && typeof err.error === 'object' && err.error.message) {
      return err.error.message;
    }
    if (typeof err.error === 'string' && err.error) {
      return err.error;
    }
    return repli;
  }
}
