import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { Users } from '../_model/users';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { NotificationService } from '../_service/notification.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-book-details',
  templateUrl: './book-details.component.html',
  styleUrls: ['./book-details.component.css']
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
    public userService: UsersService
  ) { }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['bookId'];
    this.book = new Books();
    this.bookService.getBookById(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.book = data,
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
      next: (data) => this.borrow = data,
      error: () => this.notificationService.showError('Erreur de chargement de l\'historique')
    });
  }

  public getUserData(userId: number): string {
    if (this.user && this.user.userId === userId && this.user.name) {
      return this.user.name;
    }
    this.userService.getUserById(userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.user = data,
      error: () => this.notificationService.showError('Erreur de chargement de l\'utilisateur')
    });
    return '';
  }
}
