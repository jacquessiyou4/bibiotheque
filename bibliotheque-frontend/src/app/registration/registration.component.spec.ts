import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { RegistrationComponent } from './registration.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';

describe('RegistrationComponent', () => {
  let component: RegistrationComponent;
  let fixture: ComponentFixture<RegistrationComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let router: Router;

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['createUser']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ RegistrationComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
      ]
    })
    .compileComponents();

    usersServiceSpy.createUser.and.returnValue(of({ username: 'nouveau' }));

    fixture = TestBed.createComponent(RegistrationComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('le rôle sélectionné par défaut est User (ADHERENT)', () => {
    expect(component.selectedRole).toBe('User');
  });

  it('onSubmit applique le rôle choisi puis crée l\u2019utilisateur', () => {
    component.user.username = 'nouveau';
    component.user.password = 'secret';
    component.selectedRole = 'Admin';
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(component.user.role).toEqual([{ roleName: 'Admin' }]);
    expect(usersServiceSpy.createUser).toHaveBeenCalledWith(component.user);
    expect(router.navigate).toHaveBeenCalledWith(['/users']);
  });

  it('togglePasswordVisibility bascule la visibilité du mot de passe', () => {
    expect(component.showPassword).toBe(false);

    component.togglePasswordVisibility();
        expect(component.showPassword).toBe(true);
  });
});