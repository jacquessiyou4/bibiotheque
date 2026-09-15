import { Component, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Books } from '../../../shared/models/books';
import { BooksService } from '../../../core/api/books.service';
import { NotificationService } from '../../../core/services/notification.service';

// OnPush sans markForCheck : la vue ne change qu'à la saisie, la réponse
// HTTP navigue ou passe par les notifications.
@Component({
  selector: 'app-create-book',
  templateUrl: './create-book.component.html',
  styleUrls: ['./create-book.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateBookComponent implements OnDestroy {

  private destroy$ = new Subject<void>();

  book: Books = new Books();
  constructor(private booksService: BooksService,
    private notificationService: NotificationService,
    private router: Router) { }


  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  saveBook() {
    this.booksService.createBook(this.book).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.goToBooksList(),
      error: () => this.notificationService.showError('Erreur lors de la création du livre')
    });
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

  onSubmit() {
    this.saveBook();
  }

}
