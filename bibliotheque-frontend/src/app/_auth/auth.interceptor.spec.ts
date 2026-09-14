import { HttpErrorResponse, HttpHandler, HttpRequest, HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { AuthInterceptor } from './auth.interceptor';
import { UserAuthService } from '../_service/user-auth.service';
import { NotificationService } from '../_service/notification.service';

describe('AuthInterceptor', () => {
  let interceptor: AuthInterceptor;
  let userAuthService: UserAuthService;
  let router: Router;
  let handler: jasmine.SpyObj<HttpHandler>;
  let notificationService: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule]
    });
    userAuthService = TestBed.inject(UserAuthService);
    router = TestBed.inject(Router);
    notificationService = jasmine.createSpyObj('NotificationService', ['showWarning']);
    interceptor = new AuthInterceptor(userAuthService, router, notificationService);
    handler = jasmine.createSpyObj('HttpHandler', ['handle']);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('ajoute le jeton Bearer aux requêtes ordinaires', () => {
    userAuthService.setToken('jeton-de-test');
    let requeteRecue: HttpRequest<any> | undefined;
    handler.handle.and.callFake((req: HttpRequest<any>) => {
      requeteRecue = req;
      return of(new HttpResponse({ status: 200 }));
    });

    interceptor.intercept(new HttpRequest('GET', '/admin/books'), handler)
      .subscribe(() => undefined);

        const auth = requeteRecue!.headers.get('Authorization');
    expect(auth).toBe('Bearer jeton-de-test');
  });

  it('ne touche pas aux requêtes marquées No-Auth (login Keycloak)', () => {
    const noAuth = new HttpRequest('POST', '/realms/bibliotheque/token', { username: 'A1' })
      .clone({ setHeaders: { 'No-Auth': 'True' } });
    let requeteRecue: HttpRequest<any> | undefined;
    handler.handle.and.callFake((req: HttpRequest<any>) => {
      requeteRecue = req;
      return of(new HttpResponse({ status: 200 }));
    });

    interceptor.intercept(noAuth, handler).subscribe(() => undefined);

        const auth = requeteRecue!.headers.get('Authorization');
    expect(auth).toBeNull();
  });

  it('redirige vers /login sur une erreur 401', () => {
    userAuthService.setToken('jeton');
    spyOn(router, 'navigate');
    handler.handle.and.returnValue(throwError(new HttpErrorResponse({ status: 401 })));

    interceptor.intercept(new HttpRequest('GET', '/me'), handler).subscribe({
      error: () => undefined
    });

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('jeton expiré (401) : efface la session et affiche le message d’expiration du backend', () => {
    userAuthService.setToken('jeton-expire');
    userAuthService.setRoles([{ roleName: 'ADHERENT' }]);
    spyOn(router, 'navigate');
    handler.handle.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 401,
      error: { message: 'Votre session a expiré. Veuillez vous reconnecter.' }
    })));

    interceptor.intercept(new HttpRequest('GET', '/api/reservations'), handler).subscribe({
      error: () => undefined
    });

    expect(userAuthService.getToken()).toBeNull();
    expect(notificationService.showWarning).toHaveBeenCalledWith('Votre session a expiré. Veuillez vous reconnecter.');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('401 au login (requête No-Auth) : pas de message d’expiration de session', () => {
    spyOn(router, 'navigate');
    handler.handle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const login = new HttpRequest('POST', '/realms/bibliotheque/token', {})
      .clone({ setHeaders: { 'No-Auth': 'True' } });

    interceptor.intercept(login, handler).subscribe({ error: () => undefined });

    expect(notificationService.showWarning).not.toHaveBeenCalled();
  });

  it('redirige vers /forbidden sur une erreur 403', () => {
    userAuthService.setToken('jeton');
    spyOn(router, 'navigate');
    handler.handle.and.returnValue(throwError(new HttpErrorResponse({ status: 403 })));

    interceptor.intercept(new HttpRequest('GET', '/me'), handler).subscribe({
      error: () => undefined
    });

    expect(router.navigate).toHaveBeenCalledWith(['/forbidden']);
  });

  it('propage l\u2019erreur sans rediriger pour un autre statut (409)', () => {
    userAuthService.setToken('jeton');
    spyOn(router, 'navigate');
    const erreur = new HttpErrorResponse({ status: 409 });
    handler.handle.and.returnValue(throwError(erreur));
    let erreurRecue: HttpErrorResponse | undefined;

    interceptor.intercept(new HttpRequest('GET', '/api/reservations'), handler)
      .subscribe({ error: (e: HttpErrorResponse) => erreurRecue = e });

    expect(router.navigate).not.toHaveBeenCalled();
    expect(erreurRecue).toBe(erreur);
  });
});
