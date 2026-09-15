import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/services/translation.service';
import { UsersService } from '../../../core/api/users.service';
import { UserAuthService } from '../../../core/services/user-auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Profile, TokenResponse } from '../../../shared/models/auth';

describe('LoginComponent', () => {
  const MESSAGE_AUTRE_ECHEC = 'Connexion impossible : compte inconnu de l\'application ou serveur injoignable';

  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let userAuthServiceSpy: jasmine.SpyObj<UserAuthService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  function jetons(accessToken: string): TokenResponse {
    return {
      accessToken, refreshToken: 'refresh', tokenType: 'Bearer',
      expiresIn: 1800, refreshExpiresIn: 1800, scope: 'profile email'
    };
  }

  function profil(userId: number, name: string): Profile {
    return { userId, username: 'u' + userId, name, email: null, roles: [] };
  }

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['login', 'getProfile']);
    userAuthServiceSpy = jasmine.createSpyObj('UserAuthService',
      ['decodeJwt', 'setRoles', 'setToken', 'setName', 'setUserId', 'clear']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ LoginComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: UserAuthService, useValue: userAuthServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('togglePasswordVisibility bascule la visibilité du mot de passe', () => {
    expect(component.showPassword).toBe(false);

    component.togglePasswordVisibility();
    expect(component.showPassword).toBe(true);

    component.togglePasswordVisibility();
    expect(component.showPassword).toBe(false);
  });

  it('login stocke l’accessToken de POST /auth/token, les rôles du jeton, le profil, puis navigue', () => {
    usersServiceSpy.login.and.returnValue(of(jetons('jeton-keycloak')));
    usersServiceSpy.getProfile.and.returnValue(of(profil(2, 'Admin Bibliothèque')));
    userAuthServiceSpy.decodeJwt.and.returnValue({
      realm_access: { roles: ['Admin', 'ADHERENT'] },
      preferred_username: 'admin'
    } as never);
    spyOn(router, 'navigate');
    const fauxFormulaire = {} as never;

    component.login(fauxFormulaire);

    expect(usersServiceSpy.login).toHaveBeenCalledWith(fauxFormulaire);
    expect(userAuthServiceSpy.decodeJwt).toHaveBeenCalledWith('jeton-keycloak');
    expect(userAuthServiceSpy.setToken).toHaveBeenCalledWith('jeton-keycloak');
    expect(userAuthServiceSpy.setRoles).toHaveBeenCalledWith(
      [{ roleName: 'Admin' }, { roleName: 'ADHERENT' }]);
    expect(userAuthServiceSpy.setUserId).toHaveBeenCalledWith(2);
    expect(userAuthServiceSpy.setName).toHaveBeenCalledWith('Admin Bibliothèque');
    expect(router.navigate).toHaveBeenCalledWith(['/books']);
  });

  it('le refreshToken n’est pas stocké (pas de renouvellement côté backend)', () => {
    usersServiceSpy.login.and.returnValue(of(jetons('jeton-acces')));
    usersServiceSpy.getProfile.and.returnValue(of(profil(1, 'Adherent')));
    userAuthServiceSpy.decodeJwt.and.returnValue({ realm_access: { roles: ['User'] } } as never);
    spyOn(router, 'navigate');

    component.login({} as never);

    expect(userAuthServiceSpy.setToken).toHaveBeenCalledOnceWith('jeton-acces');
  });

  it('login sans rôle Admin redirige vers la page des emprunts', () => {
    usersServiceSpy.login.and.returnValue(of(jetons('jeton-adherent')));
    usersServiceSpy.getProfile.and.returnValue(of(profil(1, 'Adherent')));
    userAuthServiceSpy.decodeJwt.and.returnValue({
      realm_access: { roles: ['User', 'ADHERENT'] },
      preferred_username: 'A1'
    } as never);
    spyOn(router, 'navigate');

    component.login({} as never);

    expect(router.navigate).toHaveBeenCalledWith(['/borrow-book']);
  });

  it('login avec un jeton sans rôles stocke une liste de rôles vide', () => {
    usersServiceSpy.login.and.returnValue(of(jetons('jeton-sans-role')));
    usersServiceSpy.getProfile.and.returnValue(of(profil(4, 'Sans Rôle')));
    userAuthServiceSpy.decodeJwt.and.returnValue({ preferred_username: 'x' } as never);
    spyOn(router, 'navigate');

    component.login({} as never);

    expect(userAuthServiceSpy.setRoles).toHaveBeenCalledWith([]);
    expect(router.navigate).toHaveBeenCalledWith(['/borrow-book']);
  });

  describe('échecs de connexion', () => {
    it('mauvais identifiants (401 de /auth/token) : message dédié, session effacée, pas de navigation', () => {
      usersServiceSpy.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
      spyOn(router, 'navigate');

      component.login({} as never);

      expect(userAuthServiceSpy.clear).toHaveBeenCalled();
      expect(notificationSpy.showError).toHaveBeenCalledWith('Identifiants incorrects');
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('Keycloak indisponible (503 de /auth/token) : message dédié', () => {
      usersServiceSpy.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 503 })));

      component.login({} as never);

      expect(userAuthServiceSpy.clear).toHaveBeenCalled();
      expect(notificationSpy.showError)
        .toHaveBeenCalledWith('Service d\'authentification indisponible, réessayez plus tard');
    });

    it('compte inconnu de l’application (/profile en 404) : efface le jeton déjà stocké', () => {
      usersServiceSpy.login.and.returnValue(of(jetons('jeton-valide')));
      usersServiceSpy.getProfile.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
      userAuthServiceSpy.decodeJwt.and.returnValue({ realm_access: { roles: ['User'] } } as never);
      spyOn(router, 'navigate');

      component.login({} as never);

      // Le jeton et les rôles ont été stockés avant /profile : ils doivent être effacés.
      expect(userAuthServiceSpy.setToken).toHaveBeenCalledWith('jeton-valide');
      expect(userAuthServiceSpy.clear).toHaveBeenCalled();
      expect(notificationSpy.showError).toHaveBeenCalledWith(MESSAGE_AUTRE_ECHEC);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('serveur injoignable (statut 0) : message de connexion impossible', () => {
      usersServiceSpy.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

      component.login({} as never);

      expect(userAuthServiceSpy.clear).toHaveBeenCalled();
      expect(notificationSpy.showError).toHaveBeenCalledWith(MESSAGE_AUTRE_ECHEC);
    });
  });
});
