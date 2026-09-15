import { Component } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router, UrlSegment } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { routes } from './app-routing.module';
import { premierSegmentParmi } from './core/routing/chemins';
import { AuthGuard } from './core/auth/auth.guard';
import { TranslationService } from './core/services/translation.service';
import { CHEMINS_CATALOGUE } from './features/catalogue/catalogue.module';
import { CHEMINS_EMPRUNTS } from './features/emprunts/emprunts.module';
import { CHEMINS_RESERVATIONS } from './features/reservations/reservations.module';
import { CHEMINS_UTILISATEURS } from './features/utilisateurs/utilisateurs.module';
import { CHEMINS_AUTH } from './features/auth/auth.module';
import { BooksListComponent } from './features/catalogue/books-list/books-list.component';
import { UsersListComponent } from './features/utilisateurs/users-list/users-list.component';
import { BorrowListComponent } from './features/emprunts/borrow-list/borrow-list.component';
import { ReservationsComponent } from './features/reservations/reservations/reservations.component';
import { LoginComponent } from './features/auth/login/login.component';

@Component({ template: '<router-outlet></router-outlet>' })
class HoteComponent { }

describe('Routage de l’application', () => {

  const segments = (...chemins: string[]) => chemins.map(p => new UrlSegment(p, {}));

  it('le matcher accepte les écrans de la fonctionnalité sans consommer de segment', () => {
    const matcher = premierSegmentParmi(['books', 'create-book']);

    expect(matcher(segments('books'), null as never, null as never)).toEqual({ consumed: [] });
    expect(matcher(segments('update-book', '3'), null as never, null as never)).toBeNull();
    expect(matcher([], null as never, null as never)).toBeNull();
  });

  it('les chemins du routeur racine correspondent aux routes déclarées par chaque module', () => {
    expect(routes.filter(r => r.matcher).length).toBe(5);
    const tous = [...CHEMINS_CATALOGUE, ...CHEMINS_EMPRUNTS, ...CHEMINS_RESERVATIONS, ...CHEMINS_UTILISATEURS, ...CHEMINS_AUTH];
    for (const chemin of tous) {
      const correspondances = routes.filter(r => r.matcher && r.matcher(segments(chemin), null as never, null as never));
      expect(correspondances.length).withContext(chemin).toBe(1);
    }
  });

  describe('chargement à la demande', () => {
    let router: Router;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [RouterTestingModule.withRoutes(routes), HttpClientTestingModule],
        declarations: [HoteComponent],
        providers: [
          // Les gardes laissent passer : on vérifie l'aiguillage, pas les rôles.
          { provide: AuthGuard, useValue: { canActivate: () => true } },
          { provide: TranslationService, useValue: { translate: (cle: string) => cle, getLang: () => 'fr' } },
        ]
      });
      router = TestBed.inject(Router);
    });

    const casDeNavigation: [string, unknown][] = [
      ['/books', BooksListComponent],
      ['/users', UsersListComponent],
      ['/borrow-list', BorrowListComponent],
      ['/reservations', ReservationsComponent],
      ['/login', LoginComponent],
    ];

    for (const [url, composant] of casDeNavigation) {
      it(`${url} charge son module et affiche le bon écran`, fakeAsync(() => {
        const hote = TestBed.createComponent(HoteComponent);
        router.navigateByUrl(url);
        tick();
        hote.detectChanges();
        tick();

        expect(router.url).toBe(url);
        const actif = router.routerState.snapshot.root.firstChild?.firstChild?.component
          ?? router.routerState.snapshot.root.firstChild?.component;
        expect(actif).toBe(composant as never);
      }));
    }
  });
});
