import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../core/auth/auth.guard';
import { UsersListComponent } from './users-list/users-list.component';
import { UserDetailsComponent } from './user-details/user-details.component';
import { UpdateUserComponent } from './update-user/update-user.component';
import { RegistrationComponent } from './registration/registration.component';

export const CHEMINS_UTILISATEURS = ['users', 'register-user', 'user-details', 'update-user'];

const routes: Routes = [
  { path: 'users', component: UsersListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'register-user', component: RegistrationComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'user-details/:userId', component: UserDetailsComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'update-user/:userId', component: UpdateUserComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
];

/** Utilisateurs : gestion des comptes par le bibliothécaire. */
@NgModule({
  declarations: [UsersListComponent, UserDetailsComponent, UpdateUserComponent, RegistrationComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class UtilisateursModule { }
