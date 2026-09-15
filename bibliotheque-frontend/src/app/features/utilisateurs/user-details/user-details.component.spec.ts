import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { UserDetailsComponent } from './user-details.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/services/translation.service';
import { UsersService } from '../../../core/api/users.service';
import { BorrowService } from '../../../core/api/borrow.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Users } from '../../../shared/models/users';
import { Borrow } from '../../../shared/models/borrow';

describe('UserDetailsComponent', () => {
  let component: UserDetailsComponent;
  let fixture: ComponentFixture<UserDetailsComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;

  const mockUser: Users = {
    userId: 1,
    username: 'john',
    name: 'John Doe',
    role: [{ roleName: 'ADHERENT' }]
  };

  const mockBorrows: Borrow[] = [
    { borrowId: 10, bookId: 3, userId: 1, issueDate: '2026-09-01T10:00:00', returnDate: null, dueDate: '2026-09-08T10:00:00' },
    { borrowId: 11, bookId: 5, userId: 1, issueDate: '2026-09-02T10:00:00', returnDate: '2026-09-06T10:00:00', dueDate: '2026-09-09T10:00:00' },
  ];

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUserById']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBooksBorrowedByUser']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ UserDetailsComponent, TranslatePipe ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { userId: '1' } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    usersServiceSpy.getUserById.and.returnValue(of(mockUser));
    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(of(mockBorrows));

    fixture = TestBed.createComponent(UserDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit charge l’utilisateur depuis le service', () => {
    expect(usersServiceSpy.getUserById).toHaveBeenCalledWith(1);
    expect(component.user.username).toBe('john');
    expect(component.user.name).toBe('John Doe');
  });

  it('ngOnInit charge l’historique des emprunts de l’utilisateur', () => {
    expect(borrowServiceSpy.getBooksBorrowedByUser).toHaveBeenCalledWith(1);
    expect(component.borrow.length).toBe(2);
    expect(component.borrow[0].borrowId).toBe(10);
  });

  it('affiche « non rendu » pour un emprunt en cours et la date pour un emprunt rendu', () => {
    const lignes = fixture.debugElement.queryAll(By.css('tbody tr'));

    expect(lignes.length).toBe(2);
    expect(lignes[0].nativeElement.textContent).toContain('history.notReturned');
    expect(lignes[1].nativeElement.textContent).toContain('06/09/2026');
  });

  it('signale une erreur si l’utilisateur ne peut pas être chargé', () => {
    usersServiceSpy.getUserById.and.returnValue(throwError(() => new Error('404')));

    TestBed.createComponent(UserDetailsComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement de l\'utilisateur');
  });

  it('signale une erreur si l’historique des emprunts ne peut pas être chargé', () => {
    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(throwError(() => new Error('500')));

    TestBed.createComponent(UserDetailsComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des emprunts');
  });
});
