import { Component, OnInit, OnDestroy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { UserAuthService } from '../_service/user-auth.service';
import { NotificationService } from '../_service/notification.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  showPassword = false;

  constructor(private userService: UsersService,
    private userAuthSerivce: UserAuthService,
    private notificationService: NotificationService,
    private router: Router
  ) { }

  ngOnInit() {
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  login(loginForm: NgForm) {
    this.userService.login(loginForm).pipe(
      takeUntil(this.destroy$),
      switchMap((response: any) => {
        const accessToken = response.access_token;
        const payload = this.userAuthSerivce.decodeJwt(accessToken);
        const roles: string[] = (payload.realm_access && payload.realm_access.roles) || [];

        this.userAuthSerivce.setRoles(roles.map((r) => ({ roleName: r })));
        this.userAuthSerivce.setToken(accessToken);
        this.userAuthSerivce.setName(payload.name || payload.preferred_username);

        return this.userService.getMe().pipe(
          switchMap((me: any) => {
            this.userAuthSerivce.setUserId(me.userId);
            this.userAuthSerivce.setName(me.name);
            this.navigateAfterLogin(roles);
            return of(null);
          })
        );
      })
    ).subscribe({
      error: () => this.notificationService.showError('Identifiants incorrects')
    });
  }

  private navigateAfterLogin(roles: string[]) {
    if (roles.indexOf('Admin') !== -1) {
      this.router.navigate(['/books']);
    } else {
      this.router.navigate(['/borrow-book']);
    }
  }

}