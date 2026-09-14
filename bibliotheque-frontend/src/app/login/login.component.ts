import { Component, OnInit, OnDestroy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { Profile, TokenResponse } from '../_model/auth';
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
      switchMap((response: TokenResponse) => {
        const accessToken = response.accessToken;
        // Les rôles viennent du jeton et non de /profile : ce sont eux que le
        // backend vérifie (@PreAuthorize), les gardes Angular doivent coïncider.
        const payload = this.userAuthSerivce.decodeJwt(accessToken);
        const roles: string[] = (payload.realm_access && payload.realm_access.roles) || [];

        this.userAuthSerivce.setRoles(roles.map((r) => ({ roleName: r })));
        this.userAuthSerivce.setToken(accessToken);
        this.userAuthSerivce.setName(payload.name || payload.preferred_username);

        return this.userService.getProfile().pipe(
          switchMap((profile: Profile) => {
            this.userAuthSerivce.setUserId(profile.userId);
            this.userAuthSerivce.setName(profile.name);
            this.navigateAfterLogin(roles);
            return of(null);
          })
        );
      })
    ).subscribe({
      error: (err) => {
        // Le jeton et les rôles sont stockés avant l'appel à /profile : en cas
        // d'échec, ne pas laisser une session à moitié ouverte.
        this.userAuthSerivce.clear();
        this.notificationService.showError(this.messageEchec(err?.status));
      }
    });
  }

  private messageEchec(status: number | undefined): string {
    if (status === 401) {
      return 'Identifiants incorrects';
    }
    if (status === 503) {
      // Le backend n'arrive pas à joindre Keycloak.
      return 'Service d\'authentification indisponible, réessayez plus tard';
    }
    return 'Connexion impossible : compte inconnu de l\'application ou serveur injoignable';
  }

  private navigateAfterLogin(roles: string[]) {
    if (roles.indexOf('Admin') !== -1) {
      this.router.navigate(['/books']);
    } else {
      this.router.navigate(['/borrow-book']);
    }
  }

}
