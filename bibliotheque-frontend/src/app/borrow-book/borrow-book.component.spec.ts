import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { BorrowBookComponent } from './borrow-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { NotificationService } from '../_service/notification.service';
import { Books } from '../_model/books';

describe('BorrowBookComponent', () => {
  let component: BorrowBookComponent;
  let fixture: ComponentFixture<BorrowBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['borrowBook']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showSuccess', 'showError']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ BorrowBookComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } as never },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    booksServiceSpy.getBooksList.and.returnValue(of([
      { bookId: 3, bookName: 'L2', noOfCopies: 2 } as Books
    ]));

    fixture = TestBed.createComponent(BorrowBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('charge le catalogue au démarrage', () => {
    expect(component.books.length).toBe(1);
    expect(component.books[0].bookId).toBe(3);
  });

  it('borrowBook envoie l’emprunt avec l’userId du jeton et le bookId cliqué', () => {
    borrowServiceSpy.borrowBook.and.returnValue(of('message'));

    component.borrowBook(3);

    expect(borrowServiceSpy.borrowBook).toHaveBeenCalledWith(
      jasmine.objectContaining({ bookId: 3, userId: 1 }));
  });

  it('borrowBook réussi : confirme l’emprunt et recharge le catalogue (stock à jour)', () => {
    borrowServiceSpy.borrowBook.and.returnValue(of('Adhérent Un a emprunté une copie de "L2" !'));

    component.borrowBook(3);

    expect(notificationSpy.showSuccess).toHaveBeenCalledWith('Emprunt réussi');
    expect(booksServiceSpy.getBooksList).toHaveBeenCalledTimes(2);
  });

  it('borrowBook refusé : affiche le message du backend (ex. stock épuisé)', () => {
    borrowServiceSpy.borrowBook.and.returnValue(throwError(() => ({
      status: 400, error: { message: 'Le livre "L2" n\'est plus disponible.' }
    })));

    component.borrowBook(3);

    expect(notificationSpy.showError).toHaveBeenCalledWith('Le livre "L2" n\'est plus disponible.');
    expect(booksServiceSpy.getBooksList).toHaveBeenCalledTimes(1);
  });

  it('borrowBook gère l’erreur en cas d’échec sans message du backend', () => {
    borrowServiceSpy.borrowBook.and.returnValue(throwError(() => new Error('Échec')));

    expect(() => component.borrowBook(3)).not.toThrow();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur lors de l\'emprunt');
  });

  it('borrowBook sans utilisateur connu n’invente pas d’userId', () => {
    component.userId = null;
    borrowServiceSpy.borrowBook.and.returnValue(of('message'));

    component.borrowBook(3);

    const emprunt = borrowServiceSpy.borrowBook.calls.mostRecent().args[0];
    expect(emprunt.bookId).toBe(3);
    expect(emprunt.userId).toBeUndefined();
  });

  it('signale une erreur si le catalogue ne peut pas être chargé', () => {
    booksServiceSpy.getBooksList.and.returnValue(throwError(() => new Error('500')));

    TestBed.createComponent(BorrowBookComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des livres');
  });
});
