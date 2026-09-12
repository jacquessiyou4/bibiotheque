import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { BorrowBookComponent } from './borrow-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { Books } from '../_model/books';

describe('BorrowBookComponent', () => {
  let component: BorrowBookComponent;
  let fixture: ComponentFixture<BorrowBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['borrowBook']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ BorrowBookComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } as never },
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

  it('borrowBook envoie l\u2019emprunt avec l\u2019userId du jeton et le bookId cliqué', () => {
    borrowServiceSpy.borrowBook.and.returnValue(of('message'));

    component.borrowBook(3);

    expect(borrowServiceSpy.borrowBook).toHaveBeenCalledWith(
      jasmine.objectContaining({ bookId: 3, userId: 1 }));
  });

  it('borrowBook gère l\u2019erreur en cas d\u2019échec', () => {
    borrowServiceSpy.borrowBook.and.returnValue(throwError(() => new Error('Échec')));

    expect(() => component.borrowBook(3)).not.toThrow();
  });
});
