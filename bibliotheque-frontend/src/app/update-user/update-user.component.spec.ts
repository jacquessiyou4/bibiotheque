import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UpdateUserComponent } from './update-user.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';
import { Users } from '../_model/users';

describe('UpdateUserComponent', () => {
  let component: UpdateUserComponent;
  let fixture: ComponentFixture<UpdateUserComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let router: Router;

  const mockUser: Users = {
    userId: 1,
    username: 'john',
    name: 'John Doe',
    password: 'secret',
    role: [{ roleName: 'ADHERENT' }]
  };

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUserById', 'updateUser']);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ UpdateUserComponent, TranslatePipe ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { userId: '1' } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
      ]
    })
    .compileComponents();

    // Copie à chaque appel : onSubmit modifie user.role, et l'ordre des tests est aléatoire.
    usersServiceSpy.getUserById.and.callFake(() => of({ ...mockUser, role: [...mockUser.role] }));
    usersServiceSpy.updateUser.and.returnValue(of({}));

    fixture = TestBed.createComponent(UpdateUserComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit charge l\u2019utilisateur depuis le service', () => {
    expect(usersServiceSpy.getUserById).toHaveBeenCalledWith(1);
    expect(component.user.username).toBe('john');
    expect(component.user.name).toBe('John Doe');
  });

  it('ngOnInit définit selectedRole depuis le premier rôle de l\u2019utilisateur', () => {
    // Copie à chaque appel : onSubmit modifie user.role, et l'ordre des tests est aléatoire.
    usersServiceSpy.getUserById.and.callFake(() => of({ ...mockUser, role: [...mockUser.role] }));
    component.ngOnInit();
    expect(component.selectedRole).toBe('ADHERENT');
  });

  it('ngOnInit garde selectedRole par défaut si pas de rôle', () => {
    usersServiceSpy.getUserById.and.returnValue(of({ userId: 2, username: 'no-role', name: 'No Role', password: '', role: null } as unknown as Users));
    component.ngOnInit();
    expect(component.selectedRole).toBe('User');
  });

  it('onSubmit applique le rôle sélectionné puis met à jour et navigue', () => {
    spyOn(router, 'navigate');
    component.selectedRole = 'Admin';

    component.onSubmit();

    expect(component.user.role).toEqual([{ roleName: 'Admin' }]);
    expect(usersServiceSpy.updateUser).toHaveBeenCalledWith(1, component.user);
    expect(router.navigate).toHaveBeenCalledWith(['/users']);
  });

  it('onSubmit gère l\u2019erreur du service', () => {
    usersServiceSpy.updateUser.and.returnValue(throwError(() => new Error('Erreur')));

    expect(() => component.onSubmit()).not.toThrow();
  });

  it('goToUsersList navigue vers /users', () => {
    spyOn(router, 'navigate');

    component.goToUsersList();

    expect(router.navigate).toHaveBeenCalledWith(['/users']);
  });
});
