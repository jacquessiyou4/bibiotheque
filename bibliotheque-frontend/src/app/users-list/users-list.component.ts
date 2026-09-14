import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Users } from '../_model/users';
import { DEFAULT_PAGE_SIZE } from '../_model/page';
import { UsersService } from '../_service/users.service';
import { NotificationService } from '../_service/notification.service';

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersListComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();
  users: Users[] = [];
  /** Numéro de page courant, à partir de 0 (comme Spring Data). */
  page = 0;
  totalPages = 0;
  readonly pageSize = DEFAULT_PAGE_SIZE;

  constructor(
    private usersService: UsersService,
    private router: Router,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.getUsers();
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
    this.getUsers();
  }

  private getUsers() {
    this.usersService.getUsersPage(this.page, this.pageSize).pipe(takeUntil(this.destroy$)).subscribe({
      next: (resultat) => {
        this.users = resultat.content;
        this.totalPages = resultat.totalPages;
        // OnPush : une réponse HTTP ne marque pas la vue comme modifiée.
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.showError('Erreur de chargement des utilisateurs')
    });
  }

  userDetails(userId: number) {
    this.router.navigate(['user-details', userId ]);
  }

  updateUser(userId: number) {
    this.router.navigate(['update-user', userId ]);
  }

}
