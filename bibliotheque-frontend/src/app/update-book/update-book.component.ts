import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';

@Component({
  selector: 'app-update-book',
  templateUrl: './update-book.component.html',
  styleUrls: ['./update-book.component.css']
})
export class UpdateBookComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  bookId: number;
  book: Books = new Books();
  constructor(private booksService: BooksService,
    private route: ActivatedRoute,
    private router: Router) { }

  ngOnInit(): void {
    this.bookId = this.route.snapshot.params['bookId'];
    this.booksService.getBookById(this.bookId).pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.book = data;
    })
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit() {
    this.booksService.updateBook(this.bookId, this.book).pipe(takeUntil(this.destroy$)).subscribe( data =>{
        this.goToBooksList();
    });
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

}
