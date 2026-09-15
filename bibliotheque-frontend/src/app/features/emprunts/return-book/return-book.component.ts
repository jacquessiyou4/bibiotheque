import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../shared/models/books';
import { Borrow } from '../../../shared/models/borrow';
import { messageErreur } from '../../../core/services/api-error';
import { BooksService } from '../../../core/api/books.service';
import { BorrowService } from '../../../core/api/borrow.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserAuthService } from '../../../core/services/user-auth.service';

@Component({
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReturnBookComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  books: Books[];
  borrow: Borrow[];

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    this.getBooksByUser();
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


  private getBooksByUser() {
    if (this.userId !== null) {
      this.borrowService.getBooksBorrowedByUser(this.userId).pipe(takeUntil(this.destroy$)).subscribe({
        next: (data) => {
          this.borrow = data;
          this.cdr.markForCheck();
        },
        error: () => this.notificationService.showError('Erreur de chargement des emprunts')
      });
    }
  }

  public returnBook(borrowId: number) {
    this.borrowService.returnBook(borrowId).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.notificationService.showSuccess('Retour réussi');
        // Sans rechargement, le bouton « Rendre » restait affiché.
        this.getBooksByUser();
      },
      error: (err) => this.notificationService.showError(messageErreur(err, 'Erreur lors du retour'))
    });
  }

}
