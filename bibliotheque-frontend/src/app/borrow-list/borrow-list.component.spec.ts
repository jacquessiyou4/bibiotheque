import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { BorrowListComponent, BorrowRow } from './borrow-list.component';
import { BorrowService } from '../_service/borrow.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { Users } from '../_model/users';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';

describe('BorrowListComponent', () => {
  let component: BorrowListComponent;
  let fixture: ComponentFixture<BorrowListComponent>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;

  beforeEach(async () => {
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBorrowList']);
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUsersList']);

    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [BorrowListComponent, TranslatePipe],
      providers: [
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
      ]
    }).compileComponents();

    booksServiceSpy.getBooksList.and.returnValue(of([
      { bookId: 1, bookName: 'L1' } as Books,
      { bookId: 2, bookName: 'L2' } as Books
    ]));
    usersServiceSpy.getUsersList.and.returnValue(of([
      { userId: 1, name: 'Adherent Un' } as Users
    ]));
    borrowServiceSpy.getBorrowList.and.returnValue(of([
      emprunt(10, 1, 1, null),   // en cours -> « Emprunté »
      emprunt(11, 1, 1, new Date()) // rendu -> « Rendu »
    ]));

    fixture = TestBed.createComponent(BorrowListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function emprunt(borrowId: number, userId: number, bookId: number, returnDate: Date | null): Borrow {
    const b = new Borrow();
    b.borrowId = borrowId;
    b.userId = userId;
    b.bookId = bookId;
    b.issueDate = new Date();
    b.dueDate = new Date();
        b.returnDate = returnDate as Date;
    return b;
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('construit une ligne par emprunt : en cours et rendu', () => {
    const emprunts = component.rows.filter(r => r.borrowId !== null);
    expect(emprunts.length).toBe(2);
    expect(emprunts.map(e => e.statut)).toEqual(['Emprunté', 'Rendu']);
  });

  it('affiche les livres jamais empruntés comme disponibles', () => {
    const disponibles = component.rows.filter(r => r.statut === 'Disponible');
    // L2 (bookId 2) n'a aucun emprunt actif.
    expect(disponibles.map(d => d.bookId)).toEqual([2]);
    expect(disponibles[0].bookName).toBe('L2');
    expect(disponibles[0].borrowerName).toBe('—');
  });

  it('retourne le nom de l\u2019emprunteur du référentiel utilisateurs', () => {
        const enCours = component.rows.find(r => r.borrowId === 10);
    expect(enCours!.borrowerName).toBe('Adherent Un');
  });

  it('le filtre par statut ne conserve que les lignes correspondantes', () => {
    component.filtreStatut = 'Rendu';
    expect(component.rows.map(r => r.statut)).toEqual(['Rendu']);
  });

  it('le filtre « tous » (chaîne vide) remonte toutes les lignes', () => {
    component.filtreStatut = '';
    expect(component.rows.length).toBe(3);
  });

  it('rend la ligne de chaque emprunt dans le tableau', () => {
    const lignes = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(lignes.length).toBeGreaterThanOrEqual(3);
  });
});
