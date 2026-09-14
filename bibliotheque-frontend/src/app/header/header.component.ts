import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { ThemeService } from '../_service/theme.service';
import { TranslationService } from '../_service/translation.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HeaderComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  constructor(
    private userAuthService: UserAuthService,
    private router: Router,
    public userService: UsersService,
    public themeService: ThemeService,
    public translationService: TranslationService,
    private cdr: ChangeDetectorRef,
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
    // OnPush : la connexion (LoginComponent) et la déconnexion forcée
    // (AuthInterceptor sur 401) se font hors du header puis naviguent. À
    // chaque navigation, relire la session (nom, rôles, boutons).
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => this.cdr.markForCheck());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  public isLoggedIn() {
    return this.userAuthService.isLoggedIn();
  }

  public logout() {
    this.userAuthService.clear();
    this.router.navigate(['/']);
  }
}
