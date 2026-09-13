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
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css']
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
      next: (data) => this.books = data,
      error: () => this.notificationService.showError('Erreur de chargement des livres')
    });
  }

  
  private getBooksByUser() {
    this.borrowService.getBooksBorrowedByUser(this.userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.borrow = data,
      error: () => this.notificationService.showError('Erreur de chargement des emprunts')
    });
  }

  brw: Borrow = new Borrow();
  public returnBook(borrowId: number) {
    this.brw.borrowId = borrowId;
    this.borrowService.returnBook(this.brw).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.notificationService.showSuccess('Retour réussi'),
      error: () => this.notificationService.showError('Erreur lors du retour')
    });
  }

}
