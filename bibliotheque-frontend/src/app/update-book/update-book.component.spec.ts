import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { UpdateBookComponent } from './update-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { Books } from '../_model/books';

describe('UpdateBookComponent', () => {
  let component: UpdateBookComponent;
  let fixture: ComponentFixture<UpdateBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let router: Router;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBookById', 'updateBook']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ UpdateBookComponent, TranslatePipe ],
      providers: [
                { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: 1 } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
      ]
    })
    .compileComponents();

            booksServiceSpy.getBookById.and.returnValue(of({
      bookId: 1, bookName: 'L1', bookAuthor: 'Auteur 1', noOfCopies: 2
    } as Books));

    fixture = TestBed.createComponent(UpdateBookComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('précharge le livre depuis le paramètre de route', () => {
    expect(booksServiceSpy.getBookById).toHaveBeenCalledWith(1);
    expect(component.book.bookName).toBe('L1');
  });

  it('onSubmit envoie la mise à jour du livre puis retourne à la liste', () => {
    component.book.bookName = 'Titre Modifié';
        booksServiceSpy.updateBook.and.returnValue(of({ bookId: 1 }));
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(booksServiceSpy.updateBook).toHaveBeenCalledWith(1, component.book);
    expect(router.navigate).toHaveBeenCalledWith(['/books']);
  });
});