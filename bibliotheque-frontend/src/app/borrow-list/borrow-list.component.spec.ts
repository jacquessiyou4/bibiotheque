import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { BorrowListComponent } from './borrow-list.component';
import { BorrowService } from '../_service/borrow.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { NotificationService } from '../_service/notification.service';
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
  let notificationSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBorrowList']);
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUsersList']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);

    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [BorrowListComponent, TranslatePipe],
      providers: [
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
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

  it('devrait être créé', () => {
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

  it('retourne le nom de l’emprunteur du référentiel utilisateurs', () => {
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

  it('garde telles quelles les dates déjà formatées par le backend (dd-MM-yyyy)', () => {
    borrowServiceSpy.getBorrowList.and.returnValue(of([
      { borrowId: 20, bookId: 1, userId: 1, issueDate: '01-09-2026', dueDate: '08-09-2026', returnDate: null } as Borrow
    ]));

    const autre = TestBed.createComponent(BorrowListComponent);
    autre.detectChanges();

    const ligne = autre.componentInstance.rows.find(r => r.borrowId === 20)!;
    expect(ligne.issueDate).toBe('01-09-2026');
    expect(ligne.dueDate).toBe('08-09-2026');
    expect(ligne.returnDate).toBeNull();
  });

  it('utilise un libellé de repli pour un livre ou un emprunteur absent des référentiels', () => {
    borrowServiceSpy.getBorrowList.and.returnValue(of([emprunt(30, 99, 42, null)]));

    const autre = TestBed.createComponent(BorrowListComponent);
    autre.detectChanges();

    const ligne = autre.componentInstance.rows.find(r => r.borrowId === 30)!;
    expect(ligne.bookName).toBe('Livre #42');
    expect(ligne.borrowerName).toBe('Utilisateur #99');
  });

  it('signale une erreur si l’un des chargements échoue', () => {
    usersServiceSpy.getUsersList.and.returnValue(throwError(() => new Error('403')));

    const autre = TestBed.createComponent(BorrowListComponent);
    autre.detectChanges();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur de chargement des emprunts');
    expect(autre.componentInstance.rows).toEqual([]);
  });
});
