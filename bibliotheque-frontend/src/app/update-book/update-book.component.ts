import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';
import { NotificationService } from '../_service/notification.service';

@Component({
  selector: 'app-update-book',
  templateUrl: './update-book.component.html',
  styleUrls: ['./update-book.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UpdateBookComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  bookId: number;
  book: Books = new Books();
  constructor(private booksService: BooksService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.bookId = this.route.snapshot.params['bookId'];
    this.booksService.getBookById(this.bookId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.book = data;
        // OnPush : une réponse HTTP ne marque pas la vue comme modifiée.
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement du livre')
    })
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit() {
    this.booksService.updateBook(this.bookId, this.book).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.goToBooksList(),
      error: () => this.notificationService.showError('Erreur lors de la mise à jour du livre')
    });
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

}
