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
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserDetailsComponent implements OnInit, OnDestroy {

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
    this.id = +this.route.snapshot.params['userId'];
    this.user = new Users();
    this.userService.getUserById(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.user = data;
        // OnPush : une réponse HTTP ne marque pas la vue comme modifiée.
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement de l\'utilisateur')
    })

    this.getBorrowedByUser(this.id);

  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private getBorrowedByUser(userId: number) {
    this.borrowService.getBooksBorrowedByUser(userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.borrow = data;
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement des emprunts')
    });
  }

}
