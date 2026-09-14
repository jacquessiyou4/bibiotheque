import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';
import { NotificationService } from '../_service/notification.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BooksListComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();
  books: Books[] = [];

  constructor(
    private booksService: BooksService,
    private router: Router,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) { }

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

  updateBook(bookId: number) {
    this.router.navigate(['update-book', bookId ]);
  }

  deleteBook(bookId: number) {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer ce livre ?')) {
      this.booksService.deleteBook(bookId).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => this.getBooks(),
        error: () => this.notificationService.showError('Erreur de suppression du livre')
      });
    }
  }

  bookDetails(bookId: number) {
    this.router.navigate(['book-details', bookId ]);
  }

}
