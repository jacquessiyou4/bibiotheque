import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { Borrow } from '../_model/borrow';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { BorrowService } from '../_service/borrow.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { NotificationService } from '../_service/notification.service';

export type BorrowStatut = 'Emprunté' | 'Rendu' | 'Disponible';

export interface BorrowRow {
  borrowId: number | null;
  bookId: number;
  bookName: string;
  userId: number | null;
  borrowerName: string;
  issueDate: string | null;
  dueDate: string | null;
  returnDate: string | null;
  statut: BorrowStatut;
}

@Component({
  selector: 'app-borrow-list',
  templateUrl: './borrow-list.component.html',
  styleUrls: ['./borrow-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BorrowListComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  readonly statuts: BorrowStatut[] = ['Emprunté', 'Rendu', 'Disponible'];

  allRows: BorrowRow[] = [];
  filtreStatut: BorrowStatut | '' = '';

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private usersService: UsersService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.loadBorrows();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get rows(): BorrowRow[] {
    if (!this.filtreStatut) {
      return this.allRows;
    }
    return this.allRows.filter(r => r.statut === this.filtreStatut);
  }

  private loadBorrows(): void {
    forkJoin({
      books: this.booksService.getBooksList(),
      users: this.usersService.getUsersList(),
      borrows: this.borrowService.getBorrowList()
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ books, users, borrows }) => {
        this.allRows = this.buildRows(books || [], users || [], borrows || []);
      },
      error: () => this.notificationService.showError('Erreur de chargement des emprunts')
    });
  }

  private buildRows(books: Books[], users: Users[], borrows: Borrow[]): BorrowRow[] {
    const bookName = (id: number) => books.find(b => b.bookId === id)?.bookName || `Livre #${id}`;
    const borrowerName = (id: number) => users.find(u => u.userId === id)?.name || `Utilisateur #${id}`;

    const borrowRows: BorrowRow[] = borrows.map(b => ({
      borrowId: b.borrowId,
      bookId: b.bookId,
      bookName: bookName(b.bookId),
      userId: b.userId,
      borrowerName: borrowerName(b.userId),
      issueDate: b.issueDate ? new Date(b.issueDate).toLocaleDateString() : null,
      dueDate: b.dueDate ? new Date(b.dueDate).toLocaleDateString() : null,
      returnDate: b.returnDate ? new Date(b.returnDate).toLocaleDateString() : null,
      statut: b.returnDate ? 'Rendu' : 'Emprunté'
    }));

    // Livres sans aucun emprunt en cours (jamais empruntés, ou tous leurs
    // emprunts sont rendus) : on les affiche comme "Disponible" pour que la
    // liste couvre tout le catalogue, pas seulement l'historique d'emprunt.
    const booksWithActiveBorrow = new Set(
      borrows.filter(b => !b.returnDate).map(b => b.bookId)
    );
    const availableRows: BorrowRow[] = books
      .filter(book => !booksWithActiveBorrow.has(book.bookId))
      .map(book => ({
        borrowId: null,
        bookId: book.bookId,
        bookName: book.bookName,
        userId: null,
        borrowerName: '—',
        issueDate: null,
        dueDate: null,
        returnDate: null,
        statut: 'Disponible' as BorrowStatut
      }));

    return [...borrowRows, ...availableRows];
  }
}
