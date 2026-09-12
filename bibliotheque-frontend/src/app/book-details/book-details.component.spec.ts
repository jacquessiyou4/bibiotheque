import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { BookDetailsComponent } from './book-details.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { Books } from '../_model/books';

describe('BookDetailsComponent', () => {
  let component: BookDetailsComponent;
  let fixture: ComponentFixture<BookDetailsComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBookById']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBookBorrowHistory']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ BookDetailsComponent, TranslatePipe ],
      providers: [
                { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: 1 } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
      ]
    })
    .compileComponents();

    booksServiceSpy.getBookById.and.returnValue(of({
      bookId: 1, bookName: 'L1', bookAuthor: 'Auteur 1', bookGenre: 'Roman', noOfCopies: 2
    } as Books));
    borrowServiceSpy.getBookBorrowHistory.and.returnValue(of([]));

    fixture = TestBed.createComponent(BookDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('charge le livre correspondant au paramètre de route', () => {
         expect(booksServiceSpy.getBookById).toHaveBeenCalledWith(1);
    expect(component.book.bookName).toBe('L1');
    expect(component.book.noOfCopies).toBe(2);
  });

  it('charge l\u2019historique des emprunts du livre', () => {
        expect(borrowServiceSpy.getBookBorrowHistory).toHaveBeenCalledWith(1);
  });
});