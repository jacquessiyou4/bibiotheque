import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './login/login.component';
import { LogoutComponent } from './logout/logout.component';

export const CHEMINS_AUTH = ['login', 'logout'];

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'logout', component: LogoutComponent },
];

/** Connexion et déconnexion. */
@NgModule({
  declarations: [LoginComponent, LogoutComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule { }
