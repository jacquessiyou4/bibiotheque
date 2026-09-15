import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { BorrowListComponent } from './borrow-list.component';
import { BorrowService } from '../../../core/api/borrow.service';
import { BooksService } from '../../../core/api/books.service';
import { UsersService } from '../../../core/api/users.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Books } from '../../../shared/models/books';
import { Borrow } from '../../../shared/models/borrow';
import { Users } from '../../../shared/models/users';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/services/translation.service';

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
      emprunt(11, 1, 1, '2026-09-05T10:00:00') // rendu -> « Rendu »
    ]));

    fixture = TestBed.createComponent(BorrowListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function emprunt(borrowId: number, userId: number, bookId: number, returnDate: string | null): Borrow {
    const b = new Borrow();
    b.borrowId = borrowId;
    b.userId = userId;
    b.bookId = bookId;
    b.issueDate = '2026-09-01T10:00:00';
    b.dueDate = '2026-09-08T10:00:00';
    b.returnDate = returnDate;
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

  it('garde les dates ISO du backend et les affiche en jj/mm/aaaa', () => {
    borrowServiceSpy.getBorrowList.and.returnValue(of([
      { borrowId: 20, bookId: 1, userId: 1, issueDate: '2026-09-01T10:00:00', dueDate: '2026-09-08T10:00:00', returnDate: null } as Borrow
    ]));

    const autre = TestBed.createComponent(BorrowListComponent);
    autre.detectChanges();

    const ligne = autre.componentInstance.rows.find(r => r.borrowId === 20)!;
    expect(ligne.issueDate).toBe('2026-09-01T10:00:00');
    expect(ligne.returnDate).toBeNull();
    expect(autre.nativeElement.textContent).toContain('08/09/2026');
    // Un livre sans emprunt n'a pas de date : tiret affiché.
    expect(autre.nativeElement.textContent).toContain('—');
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
