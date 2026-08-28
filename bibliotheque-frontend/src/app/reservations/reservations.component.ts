import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { Reservation, ReservationRequest, StatutReservation } from '../_model/reservation';
import { ReservationService } from '../_service/reservation.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';

type EtatEcran = 'chargement' | 'donnees' | 'vide' | 'erreur';

const STATUTS: StatutReservation[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

@Component({
  selector: 'app-reservations',
  templateUrl: './reservations.component.html',
  styleUrls: ['./reservations.component.css']
})
export class ReservationsComponent implements OnInit {

  readonly statuts = STATUTS;

  etat: EtatEcran = 'chargement';
  reservations: Reservation[] = [];
  filtreStatut: StatutReservation | '' = '';

  livres: Books[] = [];
  adherents: Users[] = [];

  creationEnCours = false;
  erreurFormulaire: string | null = null;
  resetFormulaire = 0;

  annulationEnCoursId: number | null = null;
  erreurAnnulation: string | null = null;

  constructor(
    private reservationService: ReservationService,
    private booksService: BooksService,
    private usersService: UsersService
  ) { }

  ngOnInit(): void {
    this.chargerReservations();
    this.chargerReferentiels();
  }

  chargerReservations(): void {
    this.etat = 'chargement';
    const statut = this.filtreStatut || undefined;
    this.reservationService.getReservations(statut as StatutReservation | undefined).subscribe({
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
    this.booksService.getBooksList().subscribe({
      // Le dropdown liste tous les livres, disponibles inclus : c'est ce qui
      // permet de déclencher volontairement le 409 RG-01 (voir passage devant
      // le formateur, séance 3).
      next: (livres) => this.livres = livres || [],
      error: () => this.livres = []
    });

    this.usersService.getUsersList().subscribe({
      next: (users) => this.adherents = (users || []).filter(
        (u: any) => u.role && u.role.some((r: any) => r.roleName === 'User')),
      error: () => this.adherents = []
    });
  }

  onFiltreChange(): void {
    this.chargerReservations();
  }

  onCreer(request: ReservationRequest): void {
    this.creationEnCours = true;
    this.erreurFormulaire = null;

    this.reservationService.createReservation(request).subscribe({
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
    this.reservationService.annulerReservation(id).subscribe({
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
