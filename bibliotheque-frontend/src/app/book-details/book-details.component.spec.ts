import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { BookDetailsComponent } from './book-details.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UsersService } from '../_service/users.service';
import { NotificationService } from '../_service/notification.service';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { Users } from '../_model/users';

describe('BookDetailsComponent', () => {
  let component: BookDetailsComponent;
  let fixture: ComponentFixture<BookDetailsComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBookById']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBookBorrowHistory']);
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUserById']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ BookDetailsComponent, TranslatePipe ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: 1 } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
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

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('charge le livre correspondant au paramètre de route', () => {
    expect(booksServiceSpy.getBookById).toHaveBeenCalledWith(1);
    expect(component.book.bookName).toBe('L1');
    expect(component.book.noOfCopies).toBe(2);
  });

  it('charge l’historique des emprunts du livre', () => {
    expect(borrowServiceSpy.getBookBorrowHistory).toHaveBeenCalledWith(1);
  });

  it('affiche la date de retour ou « non rendu » selon l’emprunt', () => {
    component.borrow = [
      { borrowId: 1, bookId: 1, userId: 2, issueDate: '01-09-2026', returnDate: null, dueDate: '08-09-2026' },
      { borrowId: 2, bookId: 1, userId: 3, issueDate: '02-09-2026', returnDate: '05-09-2026', dueDate: '09-09-2026' },
    ] as Borrow[];
    fixture.detectChanges();

    const lignes = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(lignes.length).toBe(2);
    expect(lignes[0].nativeElement.textContent).toContain('history.notReturned');
    expect(lignes[1].nativeElement.textContent).toContain('05-09-2026');
    expect(lignes[1].nativeElement.textContent).not.toContain('history.notReturned');
  });

  it('signale une erreur si le livre ne peut pas être chargé', () => {
    booksServiceSpy.getBookById.and.returnValue(throwError(() => new Error('404')));

    TestBed.createComponent(BookDetailsComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement du livre');
  });

  it('signale une erreur si l’historique ne peut pas être chargé', () => {
    borrowServiceSpy.getBookBorrowHistory.and.returnValue(throwError(() => new Error('403')));

    TestBed.createComponent(BookDetailsComponent).detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement de l’historique'.replace('’', '\''));
  });

  describe('getUserData', () => {
    it('renvoie le nom déjà chargé sans rappeler le service', () => {
      component.user = { userId: 2, name: 'Jean' } as Users;

      expect(component.getUserData(2)).toBe('Jean');
      expect(usersServiceSpy.getUserById).not.toHaveBeenCalled();
    });

    it('charge un autre utilisateur et renvoie une chaîne vide en attendant', () => {
      component.user = { userId: 2, name: 'Jean' } as Users;
      usersServiceSpy.getUserById.and.returnValue(of({ userId: 3, name: 'Marie' } as Users));

      expect(component.getUserData(3)).toBe('');
      expect(usersServiceSpy.getUserById).toHaveBeenCalledWith(3);
      expect(component.user.name).toBe('Marie');
    });

    it('recharge l’utilisateur si son nom est vide', () => {
      component.user = { userId: 2, name: '' } as Users;
      usersServiceSpy.getUserById.and.returnValue(of({ userId: 2, name: 'Jean' } as Users));

      component.getUserData(2);

      expect(usersServiceSpy.getUserById).toHaveBeenCalledWith(2);
    });

    it('signale une erreur si l’utilisateur ne peut pas être chargé', () => {
      usersServiceSpy.getUserById.and.returnValue(throwError(() => new Error('404')));

      expect(component.getUserData(9)).toBe('');
      expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement de l\'utilisateur');
    });
  });
});
