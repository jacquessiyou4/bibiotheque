import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CreateUserRequest } from '../_model/users';
import { NotificationService } from '../_service/notification.service';
import { UsersService } from '../_service/users.service';

// OnPush sans markForCheck : la vue ne change qu'à la saisie et aux clics,
// les réponses HTTP naviguent ou passent par les notifications.
@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegistrationComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  user: CreateUserRequest = {
    username: '',
    name: '',
    password: '',
    roles: []
  };
  showPassword = false;
  selectedRole: 'Admin' | 'User' = 'User';

  constructor(private usersService: UsersService,
    private notificationService: NotificationService,
    private router: Router) { }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  saveUser() {
    this.usersService.createUser(this.user).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.goToUsersList(),
      error: () => this.notificationService.showError('Erreur lors de la création de l\'utilisateur')
    });
  }

  goToUsersList() {
    this.router.navigate(['/users']);
  }

  onSubmit() {
    this.user.roles = [this.selectedRole];
    this.saveUser();
  }

}
