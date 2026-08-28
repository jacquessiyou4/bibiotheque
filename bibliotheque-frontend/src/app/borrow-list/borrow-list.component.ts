import { Component, OnInit } from '@angular/core';
import { Borrow } from '../_model/borrow';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { BorrowService } from '../_service/borrow.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';

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
  styleUrls: ['./borrow-list.component.css']
})
export class BorrowListComponent implements OnInit {

  readonly statuts: BorrowStatut[] = ['Emprunté', 'Rendu', 'Disponible'];

  allRows: BorrowRow[] = [];
  filtreStatut: BorrowStatut | '' = '';

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private usersService: UsersService
  ) { }

  ngOnInit(): void {
    this.loadBorrows();
  }

  get rows(): BorrowRow[] {
    if (!this.filtreStatut) {
      return this.allRows;
    }
    return this.allRows.filter(r => r.statut === this.filtreStatut);
  }

  private loadBorrows(): void {
    this.booksService.getBooksList().subscribe(books => {
      this.usersService.getUsersList().subscribe(users => {
        this.borrowService.getBorrowList().subscribe(borrows => {
          this.allRows = this.buildRows(books || [], users || [], borrows || []);
        });
      });
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
      issueDate: b.issueDate as any,
      dueDate: b.dueDate as any,
      returnDate: b.returnDate as any,
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
