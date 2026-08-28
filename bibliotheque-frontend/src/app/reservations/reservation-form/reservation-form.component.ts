import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Books } from '../../_model/books';
import { Users } from '../../_model/users';
import { ReservationRequest } from '../../_model/reservation';
import { ReservationService } from '../../_service/reservation.service';

const STATUTS_ACTIFS = ['EN_ATTENTE', 'DISPONIBLE'];
const QUOTA_RESERVATIONS_ACTIVES = 3;

@Component({
  selector: 'app-reservation-form',
  templateUrl: './reservation-form.component.html',
  styleUrls: ['./reservation-form.component.css']
})
export class ReservationFormComponent implements OnChanges {

  @Input() livres: Books[] = [];
  @Input() adherents: Users[] = [];
  @Input() errorMessage: string | null = null;
  @Input() submitting = false;
  @Input() resetTrigger: any;
  @Output() create = new EventEmitter<ReservationRequest>();

  livreId: number | null = null;
  adherentId: number | null = null;

  activeCount: number | null = null;
  loadingCount = false;

  constructor(private reservationService: ReservationService) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resetTrigger'] && !changes['resetTrigger'].firstChange) {
      this.livreId = null;
      this.adherentId = null;
      this.activeCount = null;
    }
  }

  onAdherentChange(): void {
    this.activeCount = null;
    if (!this.adherentId) { return; }

    this.loadingCount = true;
    this.reservationService.getReservationsByAdherent(this.adherentId).subscribe({
      next: (reservations) => {
        this.activeCount = reservations.filter(r => STATUTS_ACTIFS.includes(r.statut)).length;
        this.loadingCount = false;
      },
      error: () => {
        this.activeCount = null;
        this.loadingCount = false;
      }
    });
  }

  get quotaAtteint(): boolean {
    return this.activeCount !== null && this.activeCount >= QUOTA_RESERVATIONS_ACTIVES;
  }

  get formValide(): boolean {
    return !!this.livreId && !!this.adherentId;
  }

  onSubmit(): void {
    if (!this.formValide || this.submitting) { return; }
    this.create.emit({ livreId: this.livreId as number, adherentId: this.adherentId as number });
  }
}
