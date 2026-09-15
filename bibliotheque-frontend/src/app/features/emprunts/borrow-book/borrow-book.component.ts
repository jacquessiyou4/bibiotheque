import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../shared/models/books';
import { messageErreur } from '../../../core/services/api-error';
import { BooksService } from '../../../core/api/books.service';
import { BorrowService } from '../../../core/api/borrow.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserAuthService } from '../../../core/services/user-auth.service';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BorrowBookComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  books: Books[];

  constructor(
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
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
      next: (data) => {
        this.books = data;
        // OnPush : une réponse HTTP ne marque pas la vue comme modifiée.
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement des livres')
    });
  }

  borrowBook(bookId: number) {
    // Sans compte local connu, le backend refuserait la demande (userId obligatoire).
    if (this.userId === null) {
      this.notificationService.showError('Erreur lors de l\'emprunt');
      return;
    }
    this.borrowService.borrowBook({ bookId, userId: this.userId }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.notificationService.showSuccess('Emprunt réussi');
        // Le nombre d'exemplaires affiché a changé.
        this.getBooks();
      },
      error: (err) => this.notificationService.showError(messageErreur(err, 'Erreur lors de l\'emprunt'))
    });
  }
}
