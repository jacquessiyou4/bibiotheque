import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './core/layout/home/home.component';
import { ForbiddenComponent } from './core/layout/forbidden/forbidden.component';
import { premierSegmentParmi } from './core/routing/chemins';

/**
 * Seuls l'accueil et la page « accès refusé » sont chargés au démarrage.
 * Chaque fonctionnalité est un module chargé à la première visite de l'un de
 * ses écrans ; les URL (/books, /users, /borrow-book…) ne changent pas.
 * Les listes de chemins doivent correspondre aux routes de chaque module
 * (voir app-routing.module.spec.ts).
 */
export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'forbidden', component: ForbiddenComponent },
  {
    matcher: premierSegmentParmi(['books', 'create-book', 'update-book', 'book-details']),
    loadChildren: () => import('./features/catalogue/catalogue.module').then(m => m.CatalogueModule)
  },
  {
    matcher: premierSegmentParmi(['borrow-book', 'return-book', 'borrow-list']),
    loadChildren: () => import('./features/emprunts/emprunts.module').then(m => m.EmpruntsModule)
  },
  {
    matcher: premierSegmentParmi(['reservations']),
    loadChildren: () => import('./features/reservations/reservations.module').then(m => m.ReservationsModule)
  },
  {
    matcher: premierSegmentParmi(['users', 'register-user', 'user-details', 'update-user']),
    loadChildren: () => import('./features/utilisateurs/utilisateurs.module').then(m => m.UtilisateursModule)
  },
  {
    matcher: premierSegmentParmi(['login', 'logout']),
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
