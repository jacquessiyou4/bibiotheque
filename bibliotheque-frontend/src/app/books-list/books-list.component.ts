import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BooksListComponent implements OnInit {

  books: Books[] = [];

  constructor(private booksService: BooksService,
    private router: Router) { }

  ngOnInit(): void {
    this.getBooks();
  }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data =>{
      this.books = data;
    });
  }

  updateBook(bookId: number) {
    this.router.navigate(['update-book', bookId ]);
  }

  deleteBook(bookId: number) {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer ce livre ?')) {
      this.booksService.deleteBook(bookId).subscribe( data=> {
        this.getBooks();
      });
    }
  }

  bookDetails(bookId: number) {
    this.router.navigate(['book-details', bookId ]);
  }

}
