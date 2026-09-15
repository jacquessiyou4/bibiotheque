import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UsersListComponent } from './users-list.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/services/translation.service';
import { UsersService } from '../../../core/api/users.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Users } from '../../../shared/models/users';
import { Page } from '../../../shared/models/page';

describe('UsersListComponent', () => {
  let component: UsersListComponent;
  let fixture: ComponentFixture<UsersListComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  const mockUsers: Users[] = [
    { userId: 1, username: 'john', name: 'John', role: [{ roleName: 'ADHERENT' }] },
    { userId: 2, username: 'jane', name: 'Jane', role: [{ roleName: 'BIBLIOTHECAIRE' }] },
  ];

  function page(content: Users[], numero: number, totalPages: number): Page<Users> {
    return { content, number: numero, totalPages, totalElements: totalPages * 10, size: 10 };
  }

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUsersPage']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [ UsersListComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    usersServiceSpy.getUsersPage.and.returnValue(of(page(mockUsers, 0, 2)));

    fixture = TestBed.createComponent(UsersListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit charge la première page des utilisateurs', () => {
    expect(usersServiceSpy.getUsersPage).toHaveBeenCalledWith(0, 10);
    expect(component.users.length).toBe(2);
    expect(component.users[0].name).toBe('John');
    expect(component.totalPages).toBe(2);
  });

  it('affiche la pagination avec « Suivant » actif sur la première page', () => {
    const boutons = fixture.nativeElement.querySelectorAll('nav button');

    expect(fixture.nativeElement.querySelector('nav').textContent).toContain('pagination.page 1 / 2');
    expect((boutons[0] as HTMLButtonElement).disabled).toBeTrue();
    expect((boutons[1] as HTMLButtonElement).disabled).toBeFalse();
  });

  it('goToPage charge la page suivante et ignore une page hors limites', () => {
    usersServiceSpy.getUsersPage.and.returnValue(of(page([mockUsers[1]], 1, 2)));

    component.goToPage(1);
    component.goToPage(2);

    expect(usersServiceSpy.getUsersPage).toHaveBeenCalledTimes(2);
    expect(usersServiceSpy.getUsersPage.calls.mostRecent().args).toEqual([1, 10]);
    expect(component.users).toEqual([mockUsers[1]]);
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

  it('signale une erreur si la liste des utilisateurs ne peut pas être chargée', () => {
    usersServiceSpy.getUsersPage.and.returnValue(throwError(() => new Error('403')));

    const autre = TestBed.createComponent(UsersListComponent);
    autre.detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des utilisateurs');
    expect(autre.componentInstance.users).toEqual([]);
  });
});
