import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';
import { NotificationService } from '../_service/notification.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BooksListComponent implements OnInit {

  books: Books[] = [];

  constructor(
    private booksService: BooksService,
    private router: Router,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.getBooks();
  }

  private getBooks() {
    this.booksService.getBooksList().subscribe({
      next: (data) => this.books = data,
      error: () => this.notificationService.showError('Erreur de chargement des livres')
    });
  }

  updateBook(bookId: number) {
    this.router.navigate(['update-book', bookId ]);
  }

  deleteBook(bookId: number) {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer ce livre ?')) {
      this.booksService.deleteBook(bookId).subscribe({
        next: () => this.getBooks(),
        error: () => this.notificationService.showError('Erreur de suppression du livre')
      });
    }
  }

  bookDetails(bookId: number) {
    this.router.navigate(['book-details', bookId ]);
  }

}
