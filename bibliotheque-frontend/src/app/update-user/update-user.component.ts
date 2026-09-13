import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Users } from '../_model/users';
import { NotificationService } from '../_service/notification.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-update-user',
  templateUrl: './update-user.component.html',
  styleUrls: ['./update-user.component.css']
})
export class UpdateUserComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  userId: number;
  user: Users = new Users();
  selectedRole = 'User';

  constructor(private usersService: UsersService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router) { }

  ngOnInit(): void {
    this.userId = +this.route.snapshot.params['userId'];
    this.selectedRole = 'User';
    this.usersService.getUserById(this.userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.user = data;
        if (data.role && data.role.length) {
          this.selectedRole = data.role[0].roleName;
        }
      },
      error: () => this.notificationService.showError('Erreur de chargement de l\'utilisateur')
    })
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit() {
    this.user.role = [{ roleName: this.selectedRole }];
    this.usersService.updateUser(this.userId, this.user).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.goToUsersList(),
      error: () => this.notificationService.showError('Erreur lors de la mise à jour de l\'utilisateur')
    });
  }

  goToUsersList() {
    this.router.navigate(['/users']);
  }

}
