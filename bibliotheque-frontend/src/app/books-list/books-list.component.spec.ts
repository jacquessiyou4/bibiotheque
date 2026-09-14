import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { BooksListComponent } from './books-list.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { NotificationService } from '../_service/notification.service';
import { Books } from '../_model/books';
import { Page } from '../_model/page';

describe('BooksListComponent', () => {
  let component: BooksListComponent;
  let fixture: ComponentFixture<BooksListComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  function page(content: Books[], numero: number, totalPages: number): Page<Books> {
    return { content, number: numero, totalPages, totalElements: totalPages * 10, size: 10 };
  }

  const L1 = { bookId: 1, bookName: 'L1', noOfCopies: 2 } as Books;
  const L2 = { bookId: 2, bookName: 'L2', noOfCopies: 0 } as Books;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksPage', 'deleteBook']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [ BooksListComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    booksServiceSpy.getBooksPage.and.returnValue(of(page([L1, L2], 0, 3)));

    fixture = TestBed.createComponent(BooksListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('charge la première page de 10 livres au démarrage', () => {
    expect(booksServiceSpy.getBooksPage).toHaveBeenCalledWith(0, 10);
    expect(component.books.length).toBe(2);
    expect(component.books[1].bookName).toBe('L2');
    expect(component.totalPages).toBe(3);
  });

  it('affiche la pagination avec « Précédent » désactivé sur la première page', () => {
    const nav: HTMLElement = fixture.nativeElement.querySelector('nav');
    const boutons = nav.querySelectorAll('button');

    expect(nav.textContent).toContain('pagination.page 1 / 3');
    expect((boutons[0] as HTMLButtonElement).disabled).toBeTrue();
    expect((boutons[1] as HTMLButtonElement).disabled).toBeFalse();
  });

  it('n’affiche pas la pagination quand tout tient sur une page', () => {
    booksServiceSpy.getBooksPage.and.returnValue(of(page([L1], 0, 1)));

    const autre = TestBed.createComponent(BooksListComponent);
    autre.detectChanges();

    expect(autre.nativeElement.querySelector('nav')).toBeNull();
  });

  it('goToPage charge la page demandée', () => {
    booksServiceSpy.getBooksPage.and.returnValue(of(page([L2], 1, 3)));

    component.goToPage(1);

    expect(booksServiceSpy.getBooksPage).toHaveBeenCalledWith(1, 10);
    expect(component.page).toBe(1);
    expect(component.books).toEqual([L2]);
  });

  it('goToPage ignore une page hors limites', () => {
    component.goToPage(-1);
    component.goToPage(3);

    expect(booksServiceSpy.getBooksPage).toHaveBeenCalledTimes(1);
    expect(component.page).toBe(0);
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

  it('deleteBook supprime puis recharge la page courante', () => {
    // Sans ce stub, le vrai window.confirm bloque Chrome headless (déconnexion Karma).
    spyOn(window, 'confirm').and.returnValue(true);
    booksServiceSpy.deleteBook.and.returnValue(of({ deleted: true }));

    component.deleteBook(1);

    expect(booksServiceSpy.deleteBook).toHaveBeenCalledWith(1);
    expect(booksServiceSpy.getBooksPage).toHaveBeenCalledTimes(2);
    expect(booksServiceSpy.getBooksPage.calls.mostRecent().args).toEqual([0, 10]);
  });

  it('deleteBook du dernier livre d’une page revient à la page précédente', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    booksServiceSpy.getBooksPage.and.returnValue(of(page([L2], 2, 3)));
    component.goToPage(2);
    booksServiceSpy.deleteBook.and.returnValue(of({ deleted: true }));
    booksServiceSpy.getBooksPage.and.returnValues(of(page([], 2, 2)), of(page([L1], 1, 2)));

    component.deleteBook(2);

    expect(booksServiceSpy.getBooksPage.calls.mostRecent().args).toEqual([1, 10]);
    expect(component.page).toBe(1);
    expect(component.books).toEqual([L1]);
  });

  it('deleteBook ne supprime rien si l’utilisateur annule la confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteBook(1);

    expect(booksServiceSpy.deleteBook).not.toHaveBeenCalled();
    expect(booksServiceSpy.getBooksPage).toHaveBeenCalledTimes(1);
  });

  it('deleteBook signale l’échec de la suppression sans recharger la liste', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    booksServiceSpy.deleteBook.and.returnValue(throwError(() => new Error('403')));

    component.deleteBook(1);

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de suppression du livre');
    expect(booksServiceSpy.getBooksPage).toHaveBeenCalledTimes(1);
  });

  it('signale une erreur si la liste des livres ne peut pas être chargée', () => {
    booksServiceSpy.getBooksPage.and.returnValue(throwError(() => new Error('500')));

    TestBed.createComponent(BooksListComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des livres');
  });
});
