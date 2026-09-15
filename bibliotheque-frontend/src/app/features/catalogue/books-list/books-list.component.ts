import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../shared/models/books'
import { DEFAULT_PAGE_SIZE } from '../../../shared/models/page';
import { BooksService } from '../../../core/api/books.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BooksListComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();
  books: Books[] = [];
  /** Numéro de page courant, à partir de 0 (comme Spring Data). */
  page = 0;
  totalPages = 0;
  readonly pageSize = DEFAULT_PAGE_SIZE;

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

  goToPage(page: number) {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }
    this.page = page;
    this.getBooks();
  }

  private getBooks() {
    this.booksService.getBooksPage(this.page, this.pageSize).pipe(takeUntil(this.destroy$)).subscribe({
      next: (resultat) => {
        // Dernier livre d'une page supprimé : la page est vide, revenir à la précédente.
        if (resultat.content.length === 0 && this.page > 0) {
          this.goToPage(this.page - 1);
          return;
        }
        this.books = resultat.content;
        this.totalPages = resultat.totalPages;
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
