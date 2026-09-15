import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../../shared/models/books';
import { Users } from '../../../../shared/models/users';
import { ReservationRequest } from '../../../../shared/models/reservation';
import { ReservationService } from '../../../../core/api/reservation.service';

const STATUTS_ACTIFS = ['EN_ATTENTE', 'DISPONIBLE'];
const QUOTA_RESERVATIONS_ACTIVES = 3;

@Component({
  selector: 'app-reservation-form',
  templateUrl: './reservation-form.component.html',
  styleUrls: ['./reservation-form.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReservationFormComponent implements OnChanges, OnDestroy {

  private destroy$ = new Subject<void>();

  @Input() livres: Books[] = [];
  @Input() adherents: Users[] = [];
  @Input() errorMessage: string | null = null;
  @Input() submitting = false;
  @Input() resetTrigger: number | null = null;
  // Identité imposée (RS-04) : quand un ADHERENT connecté réserve pour
  // lui-même, l'adhérent n'est pas choisi dans le formulaire.
  @Input() readonlyAdherentId: number | null = null;
  @Output() create = new EventEmitter<ReservationRequest>();

  livreId: number | null = null;
  adherentId: number | null = null;

  activeCount: number | null = null;
  loadingCount = false;

  constructor(private reservationService: ReservationService,
              private cdr: ChangeDetectorRef) { }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

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
    this.reservationService.getReservationsByAdherent(this.adherentId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (reservations) => {
        this.activeCount = reservations.filter(r => STATUTS_ACTIFS.includes(r.statut)).length;
        this.loadingCount = false;
        // OnPush : une réponse HTTP ne marque pas la vue comme modifiée.
        this.cdr.markForCheck();
      },
      error: () => {
        this.activeCount = null;
        this.loadingCount = false;
        this.cdr.markForCheck();
      }
    });
  }

  get quotaAtteint(): boolean {
    return this.activeCount !== null && this.activeCount >= QUOTA_RESERVATIONS_ACTIVES;
  }

  get formValide(): boolean {
    const adherentOk = this.readonlyAdherentId !== null || !!this.adherentId;
    return !!this.livreId && adherentOk;
  }

  onSubmit(): void {
    if (!this.formValide || this.submitting) { return; }
    const adherentId = this.readonlyAdherentId ?? this.adherentId;
    this.create.emit({ livreId: this.livreId as number, adherentId: adherentId as number });
  }
}
