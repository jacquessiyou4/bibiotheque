import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { RegistrationComponent } from './registration.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';
import { NotificationService } from '../_service/notification.service';

describe('RegistrationComponent', () => {
  let component: RegistrationComponent;
  let fixture: ComponentFixture<RegistrationComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['createUser']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ RegistrationComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    usersServiceSpy.createUser.and.returnValue(of({ username: 'nouveau' }));

    fixture = TestBed.createComponent(RegistrationComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('le rôle sélectionné par défaut est User', () => {
    expect(component.selectedRole).toBe('User');
  });

  it('onSubmit applique le rôle choisi puis crée l’utilisateur', () => {
    component.user.username = 'nouveau';
    component.user.password = 'secret';
    component.selectedRole = 'Admin';
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(component.user.roles).toEqual(['Admin']);
    expect(usersServiceSpy.createUser).toHaveBeenCalledWith(component.user);
    expect(router.navigate).toHaveBeenCalledWith(['/users']);
  });

  it('onSubmit sans changer le rôle crée un compte User', () => {
    component.user.username = 'simple';
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(usersServiceSpy.createUser).toHaveBeenCalledWith(jasmine.objectContaining({ roles: ['User'] }));
  });

  it('onSubmit signale l’échec de la création (ex. username déjà utilisé) et reste sur le formulaire', () => {
    usersServiceSpy.createUser.and.returnValue(throwError(() => new Error('409')));
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur lors de la création de l\'utilisateur');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('togglePasswordVisibility bascule la visibilité du mot de passe', () => {
    expect(component.showPassword).toBe(false);

    component.togglePasswordVisibility();
    expect(component.showPassword).toBe(true);
  });
});
