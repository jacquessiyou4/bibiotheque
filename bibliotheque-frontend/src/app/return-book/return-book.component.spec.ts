import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { ReturnBookComponent } from './return-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BorrowService } from '../_service/borrow.service';
import { BooksService } from '../_service/books.service';
import { UserAuthService } from '../_service/user-auth.service';
import { NotificationService } from '../_service/notification.service';
import { Borrow } from '../_model/borrow';
import { Books } from '../_model/books';

describe('ReturnBookComponent', () => {
  let component: ReturnBookComponent;
  let fixture: ComponentFixture<ReturnBookComponent>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;

  const mockBorrows: Borrow[] = [
    { borrowId: 7, bookId: 3, userId: 1, issueDate: new Date(), returnDate: null, dueDate: new Date() },
  ];
  const mockBooks: Books[] = [
    { bookId: 3, bookName: 'L2', bookAuthor: 'A', bookGenre: 'Essai', noOfCopies: 1 } as Books,
  ];

  beforeEach(async () => {
    borrowServiceSpy = jasmine.createSpyObj('BorrowService',
      ['getBooksBorrowedByUser', 'returnBook']);
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showSuccess', 'showError']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ ReturnBookComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } as never },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(of(mockBorrows));
    booksServiceSpy.getBooksList.and.returnValue(of(mockBooks));

    fixture = TestBed.createComponent(ReturnBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('charge au démarrage la liste des emprunts de l’utilisateur connecté', () => {
    expect(borrowServiceSpy.getBooksBorrowedByUser).toHaveBeenCalledWith(1);
    expect(component.borrow.length).toBe(1);
    expect(component.borrow[0].borrowId).toBe(7);
  });

  it('charge au démarrage la liste des livres', () => {
    expect(booksServiceSpy.getBooksList).toHaveBeenCalled();
    expect(component.books.length).toBe(1);
    expect(component.books[0].bookId).toBe(3);
  });

  it('returnBook envoie le borrowId au service', () => {
    borrowServiceSpy.returnBook.and.returnValue(of({ message: 'ok' }));

    component.returnBook(7);

    expect(borrowServiceSpy.returnBook).toHaveBeenCalledWith(jasmine.objectContaining({ borrowId: 7 }));
  });

  it('returnBook réussi : confirme le retour et recharge les emprunts (bouton « Rendre » retiré)', () => {
    borrowServiceSpy.returnBook.and.returnValue(of({ borrowId: 7 }));

    component.returnBook(7);

    expect(notificationSpy.showSuccess).toHaveBeenCalledWith('Retour réussi');
    expect(borrowServiceSpy.getBooksBorrowedByUser).toHaveBeenCalledTimes(2);
  });

  it('returnBook refusé : affiche le message du backend (ex. emprunt déjà rendu)', () => {
    borrowServiceSpy.returnBook.and.returnValue(throwError(() => ({
      status: 400, error: { message: 'Cet emprunt a déjà été rendu.' }
    })));

    component.returnBook(7);

    expect(notificationSpy.showError).toHaveBeenCalledWith('Cet emprunt a déjà été rendu.');
    expect(borrowServiceSpy.getBooksBorrowedByUser).toHaveBeenCalledTimes(1);
  });

  it('returnBook gère l’erreur en cas d’échec sans message du backend', () => {
    borrowServiceSpy.returnBook.and.returnValue(throwError(() => new Error('Erreur')));

    expect(() => component.returnBook(7)).not.toThrow();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur lors du retour');
  });

  it('ne charge aucun emprunt si l’utilisateur connecté est inconnu', () => {
    borrowServiceSpy.getBooksBorrowedByUser.calls.reset();
    component.userId = null;

    component.ngOnInit();

    expect(borrowServiceSpy.getBooksBorrowedByUser).not.toHaveBeenCalled();
  });

  it('signale une erreur si les emprunts ne peuvent pas être chargés', () => {
    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(throwError(() => new Error('403')));

    TestBed.createComponent(ReturnBookComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des emprunts');
  });

  it('signale une erreur si les livres ne peuvent pas être chargés', () => {
    booksServiceSpy.getBooksList.and.returnValue(throwError(() => new Error('500')));

    TestBed.createComponent(ReturnBookComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des livres');
  });
});
