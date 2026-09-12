import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { ReturnBookComponent } from './return-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BorrowService } from '../_service/borrow.service';
import { BooksService } from '../_service/books.service';
import { UserAuthService } from '../_service/user-auth.service';
import { Borrow } from '../_model/borrow';
import { Books } from '../_model/books';

describe('ReturnBookComponent', () => {
  let component: ReturnBookComponent;
  let fixture: ComponentFixture<ReturnBookComponent>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;

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

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ ReturnBookComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } as never },
      ]
    })
    .compileComponents();

    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(of(mockBorrows));
    booksServiceSpy.getBooksList.and.returnValue(of(mockBooks));

    fixture = TestBed.createComponent(ReturnBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('charge au démarrage la liste des emprunts de l\u2019utilisateur connecté', () => {
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

  it('returnBook gère l\u2019erreur en cas d\u2019échec', () => {
    borrowServiceSpy.returnBook.and.returnValue(throwError(() => new Error('Erreur')));

    expect(() => component.returnBook(7)).not.toThrow();
  });
});
