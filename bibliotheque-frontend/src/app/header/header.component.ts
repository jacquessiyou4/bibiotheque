import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { ThemeService } from '../_service/theme.service';
import { TranslationService } from '../_service/translation.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  constructor(
    private userAuthService: UserAuthService,
    private router: Router,
    public userService: UsersService,
    public themeService: ThemeService,
    public translationService: TranslationService,
  ) { }

  // Le header n'est jamais recréé : lire le nom à chaque rendu, sinon il
  // reste celui d'avant la connexion (null) jusqu'au rechargement de la page.
  get name(): string | null {
    return this.userAuthService.getName();
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  toggleLang() {
    this.translationService.toggleLang();
  }

  ngOnInit(): void {
  }

  public isLoggedIn() {
    return this.userAuthService.isLoggedIn();
  }

  public logout() {
    this.userAuthService.clear();
    this.router.navigate(['/']);
  }
}
