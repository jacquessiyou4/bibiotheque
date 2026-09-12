import { Component, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  showPassword = false;

  constructor(private userService: UsersService,
    private userAuthSerivce: UserAuthService,
    private router: Router
  ) { }

  ngOnInit() {
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  login(loginForm: NgForm) {
    this.userService.login(loginForm).subscribe(
      (response: any) => {
        // Réponse Keycloak : { access_token, refresh_token, expires_in, … }
        const accessToken = response.access_token;
        const payload = this.userAuthSerivce.decodeJwt(accessToken);
        const roles: string[] = (payload.realm_access && payload.realm_access.roles) || [];

        // Rôles stockés au format attendu par roleMatch : [{ roleName }].
        this.userAuthSerivce.setRoles(roles.map((r) => ({ roleName: r })));
        this.userAuthSerivce.setToken(accessToken);
        this.userAuthSerivce.setName(payload.name || payload.preferred_username);

        // userId local : résolu par le backend /me à partir du jeton Keycloak.
        this.userService.getMe().subscribe(
          (me: any) => {
            this.userAuthSerivce.setUserId(me.userId);
            this.userAuthSerivce.setName(me.name);
            this.navigateAfterLogin(roles);
          },
          () => {
            this.navigateAfterLogin(roles);
          }
        );
      },
      () => {}
    );
  }

  private navigateAfterLogin(roles: string[]) {
    if (roles.indexOf('Admin') !== -1) {
      this.router.navigate(['/books']);
    } else {
      this.router.navigate(['/borrow-book']);
    }
  }

}