import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { UpdateBookComponent } from './update-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { NotificationService } from '../_service/notification.service';
import { Books } from '../_model/books';

describe('UpdateBookComponent', () => {
  let component: UpdateBookComponent;
  let fixture: ComponentFixture<UpdateBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBookById', 'updateBook']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ UpdateBookComponent, TranslatePipe ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: 1 } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
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

  it('onSubmit signale l’échec de la mise à jour et reste sur le formulaire', () => {
    booksServiceSpy.updateBook.and.returnValue(throwError(() => new Error('400')));
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur lors de la mise à jour du livre');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('signale une erreur si le livre à modifier ne peut pas être chargé', () => {
    booksServiceSpy.getBookById.and.returnValue(throwError(() => new Error('404')));

    TestBed.createComponent(UpdateBookComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement du livre');
  });
});
