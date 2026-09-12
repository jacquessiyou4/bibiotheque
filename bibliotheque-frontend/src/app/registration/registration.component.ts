import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CreateUserRequest } from '../_model/users';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
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
    this.usersService.createUser(this.user).pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.goToUsersList();
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
