import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../shared/models/books';
import { Borrow } from '../../../shared/models/borrow';
import { Users } from '../../../shared/models/users';
import { BooksService } from '../../../core/api/books.service';
import { BorrowService } from '../../../core/api/borrow.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UsersService } from '../../../core/api/users.service';

@Component({
  selector: 'app-book-details',
  templateUrl: './book-details.component.html',
  styleUrls: ['./book-details.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BookDetailsComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  id: number;
  book: Books;
  borrow: Borrow[];
  user: Users;

  constructor(private route: ActivatedRoute,
    private bookService: BooksService,
    private borrowService: BorrowService,
    private notificationService: NotificationService,
    public userService: UsersService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['bookId'];
    this.book = new Books();
    this.bookService.getBookById(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.book = data;
        // OnPush : une réponse HTTP ne marque pas la vue comme modifiée.
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement du livre')
    })

    this.getBorrowHistory(this.id);

  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private getBorrowHistory(bookId: number) {
    this.borrowService.getBookBorrowHistory(bookId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.borrow = data;
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement de l\'historique')
    });
  }

  public getUserData(userId: number): string {
    if (this.user && this.user.userId === userId && this.user.name) {
      return this.user.name;
    }
    this.userService.getUserById(userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.user = data;
        // Appelée depuis le template : le nom s'affichera au prochain rendu.
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement de l\'utilisateur')
    });
    return '';
  }
}
