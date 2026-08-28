import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Reservation } from '../../_model/reservation';
import { TranslationService } from '../../_service/translation.service';

const STATUTS_ANNULABLES = ['EN_ATTENTE', 'DISPONIBLE'];
const PAGE_SIZE = 10;

const BADGE_PAR_STATUT: { [statut: string]: string } = {
  EN_ATTENTE: 'bg-warning text-dark',
  DISPONIBLE: 'bg-success',
  ANNULEE: 'bg-secondary',
  EXPIREE: 'bg-dark',
  HONOREE: 'bg-primary',
};

@Component({
  selector: 'app-reservation-list',
  templateUrl: './reservation-list.component.html',
  styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent implements OnChanges {

  @Input() reservations: Reservation[] = [];
  @Input() cancellingId: number | null = null;
  @Output() cancel = new EventEmitter<number>();

  triParExpirationAsc = true;
  page = 1;
  pageSize = PAGE_SIZE;

  constructor(private translationService: TranslationService) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['reservations']) {
      this.page = 1;
    }
  }

  get reservationsTriees(): Reservation[] {
    const sens = this.triParExpirationAsc ? 1 : -1;
    return [...this.reservations].sort((a, b) =>
      sens * (new Date(a.dateExpiration).getTime() - new Date(b.dateExpiration).getTime()));
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.reservationsTriees.length / this.pageSize));
  }

  get reservationsPage(): Reservation[] {
    const debut = (this.page - 1) * this.pageSize;
    return this.reservationsTriees.slice(debut, debut + this.pageSize);
  }

  toggleTri(): void {
    this.triParExpirationAsc = !this.triParExpirationAsc;
  }

  pagePrecedente(): void {
    if (this.page > 1) { this.page--; }
  }

  pageSuivante(): void {
    if (this.page < this.totalPages) { this.page++; }
  }

  peutAnnuler(reservation: Reservation): boolean {
    return STATUTS_ANNULABLES.includes(reservation.statut);
  }

  badgeClasse(statut: string): string {
    return BADGE_PAR_STATUT[statut] || 'bg-secondary';
  }

  onAnnuler(reservation: Reservation): void {
    const confirme = window.confirm(
      this.translationService.translate('reservations.confirmCancel', {
        book: reservation.livreNom,
        adherent: reservation.adherentNom
      }));
    if (confirme) {
      this.cancel.emit(reservation.id);
    }
  }
}
