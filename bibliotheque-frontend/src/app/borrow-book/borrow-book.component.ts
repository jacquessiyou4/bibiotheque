import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { NotificationService } from '../_service/notification.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css']
})
export class BorrowBookComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  books: Books[];

  constructor(
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
    private notificationService: NotificationService,
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private getBooks() {
    this.booksService.getBooksList().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.books = data,
      error: () => this.notificationService.showError('Erreur de chargement des livres')
    });
  }

  borrow: Borrow = new Borrow();

  borrowBook(bookId: number) {
    this.borrow.bookId = bookId;
    this.borrow.userId = this.userId;
    this.borrowService.borrowBook(this.borrow).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.notificationService.showSuccess('Emprunt réussi'),
      error: () => this.notificationService.showError('Erreur lors de l\'emprunt')
    });
  }
}
