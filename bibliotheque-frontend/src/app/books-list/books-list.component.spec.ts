import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { BooksListComponent } from './books-list.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { Books } from '../_model/books';

describe('BooksListComponent', () => {
  let component: BooksListComponent;
  let fixture: ComponentFixture<BooksListComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let router: Router;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList', 'deleteBook']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [ BooksListComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
      ]
    })
    .compileComponents();

    booksServiceSpy.getBooksList.and.returnValue(of([
      { bookId: 1, bookName: 'L1', noOfCopies: 2 } as Books,
      { bookId: 2, bookName: 'L2', noOfCopies: 0 } as Books
    ]));

    fixture = TestBed.createComponent(BooksListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('charge la liste des livres au démarrage', () => {
    expect(booksServiceSpy.getBooksList).toHaveBeenCalled();
    expect(component.books.length).toBe(2);
    expect(component.books[1].bookName).toBe('L2');
  });

  it('updateBook redirige vers le formulaire de modification', () => {
    spyOn(router, 'navigate');

    component.updateBook(1);

    expect(router.navigate).toHaveBeenCalledWith(['update-book', 1]);
  });

  it('bookDetails redirige vers la fiche du livre', () => {
    spyOn(router, 'navigate');

    component.bookDetails(2);

    expect(router.navigate).toHaveBeenCalledWith(['book-details', 2]);
  });

  it('deleteBook supprime puis recharge la liste', () => {
    booksServiceSpy.deleteBook.and.returnValue(of({ deleted: true }));

    component.deleteBook(1);

    expect(booksServiceSpy.deleteBook).toHaveBeenCalledWith(1);
    // getBooksList appelé une seconde fois (rechargement après suppression).
    expect(booksServiceSpy.getBooksList).toHaveBeenCalledTimes(2);
  });
});
