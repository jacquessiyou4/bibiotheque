import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../core/auth/auth.guard';
import { ReservationsComponent } from './reservations/reservations.component';
import { ReservationListComponent } from './reservations/reservation-list/reservation-list.component';
import { ReservationFormComponent } from './reservations/reservation-form/reservation-form.component';

export const CHEMINS_RESERVATIONS = ['reservations'];

const routes: Routes = [
  {
    path: 'reservations', component: ReservationsComponent,
    canActivate: [AuthGuard], data: { roles: ['ADHERENT', 'BIBLIOTHECAIRE'] }
  },
];

/** Réservations des livres indisponibles. */
@NgModule({
  declarations: [ReservationsComponent, ReservationListComponent, ReservationFormComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class ReservationsModule { }
