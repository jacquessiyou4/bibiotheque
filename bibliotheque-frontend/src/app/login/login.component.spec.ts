import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';
import { UserAuthService } from '../_service/user-auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let userAuthServiceSpy: jasmine.SpyObj<UserAuthService>;
  let router: Router;

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['login', 'getMe']);
    userAuthServiceSpy = jasmine.createSpyObj('UserAuthService',
      ['decodeJwt', 'setRoles', 'setToken', 'setName', 'setUserId']);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ LoginComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: UserAuthService, useValue: userAuthServiceSpy },
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

  it('login store le token et les rôles du jeton Keycloak puis navigue', () => {
    usersServiceSpy.login.and.returnValue(of({ access_token: 'jeton-keycloak' }));
    usersServiceSpy.getMe.and.returnValue(of({ userId: 2, name: 'Admin' }));
    userAuthServiceSpy.decodeJwt.and.returnValue({
      realm_access: { roles: ['Admin', 'ADHERENT'] },
      preferred_username: 'admin'
    } as never);
    spyOn(router, 'navigate');
    const fauxFormulaire = {} as never;

    component.login(fauxFormulaire);

    expect(usersServiceSpy.login).toHaveBeenCalledWith(fauxFormulaire);
    expect(userAuthServiceSpy.setToken).toHaveBeenCalledWith('jeton-keycloak');
    expect(userAuthServiceSpy.setRoles).toHaveBeenCalledWith(
      [{ roleName: 'Admin' }, { roleName: 'ADHERENT' }]);
    expect(userAuthServiceSpy.setUserId).toHaveBeenCalledWith(2);
    expect(router.navigate).toHaveBeenCalledWith(['/books']);
  });

  it('login sans rôle Admin redirige vers la page des emprunts', () => {
    usersServiceSpy.login.and.returnValue(of({ access_token: 'jeton-adherent' }));
    usersServiceSpy.getMe.and.returnValue(of({ userId: 1, name: 'Adherent' }));
    userAuthServiceSpy.decodeJwt.and.returnValue({
      realm_access: { roles: ['User', 'ADHERENT'] },
      preferred_username: 'A1'
    } as never);
    spyOn(router, 'navigate');

    component.login({} as never);

    expect(router.navigate).toHaveBeenCalledWith(['/borrow-book']);
  });
});
