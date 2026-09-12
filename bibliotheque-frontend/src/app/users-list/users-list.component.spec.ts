import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { UsersListComponent } from './users-list.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';
import { Users } from '../_model/users';

describe('UsersListComponent', () => {
  let component: UsersListComponent;
  let fixture: ComponentFixture<UsersListComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let router: Router;

  const mockUsers: Users[] = [
    { userId: 1, username: 'john', name: 'John', password: '', role: [{ roleName: 'ADHERENT' }] },
    { userId: 2, username: 'jane', name: 'Jane', password: '', role: [{ roleName: 'BIBLIOTHECAIRE' }] },
  ];

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUsersList']);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [ UsersListComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
      ]
    })
    .compileComponents();

    usersServiceSpy.getUsersList.and.returnValue(of(mockUsers));

    fixture = TestBed.createComponent(UsersListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit charge la liste des utilisateurs', () => {
    expect(usersServiceSpy.getUsersList).toHaveBeenCalled();
    expect(component.users.length).toBe(2);
    expect(component.users[0].name).toBe('John');
  });

  it('userDetails navigue vers la page de détails', () => {
    spyOn(router, 'navigate');

    component.userDetails(1);

    expect(router.navigate).toHaveBeenCalledWith(['user-details', 1]);
  });

  it('updateUser navigue vers la page de modification', () => {
    spyOn(router, 'navigate');

    component.updateUser(2);

    expect(router.navigate).toHaveBeenCalledWith(['update-user', 2]);
  });
});
