import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { UserDetailsComponent } from './user-details.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';
import { BorrowService } from '../_service/borrow.service';
import { NotificationService } from '../_service/notification.service';
import { Users } from '../_model/users';
import { Borrow } from '../_model/borrow';

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
    { borrowId: 10, bookId: 3, userId: 1, issueDate: '01-09-2026', returnDate: null, dueDate: '08-09-2026' },
    { borrowId: 11, bookId: 5, userId: 1, issueDate: '02-09-2026', returnDate: '06-09-2026', dueDate: '09-09-2026' },
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
    expect(lignes[1].nativeElement.textContent).toContain('06-09-2026');
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
