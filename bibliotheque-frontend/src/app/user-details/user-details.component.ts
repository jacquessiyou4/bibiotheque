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
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
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
    public userService: UsersService
  ) { }

  ngOnInit(): void {
    this.id = +this.route.snapshot.params['userId'];
    this.user = new Users();
    this.userService.getUserById(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.user = data,
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
      next: (data) => this.borrow = data,
      error: () => this.notificationService.showError('Erreur de chargement des emprunts')
    });
  }

}
