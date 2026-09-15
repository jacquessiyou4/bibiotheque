import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ThemeService } from './core/services/theme.service';
import { NotificationService } from './core/services/notification.service';
import { UserAuthService } from './core/services/user-auth.service';

// OnPush : les notifications passent par le pipe async, qui marque la vue
// à chaque nouvelle valeur ; le routeur marque la vue à chaque activation.
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnDestroy {
  title = 'Library Management System';

  @ViewChild('contenu') contenu?: ElementRef<HTMLElement>;

  private destroy$ = new Subject<void>();

  constructor(private themeService: ThemeService,
              public notificationService: NotificationService,
              userAuthService: UserAuthService,
              router: Router) {
    this.themeService.init();
    // Jeton arrivé à expiration pendant que la page est ouverte : sans cela,
    // l'interface restait « connectée » jusqu'au premier refus 401 du backend.
    userAuthService.sessionExpiree$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.notificationService.showWarning('Votre session a expiré. Veuillez vous reconnecter.');
      router.navigate(['/login']);
    });
    // Après un changement d'écran, le focus clavier et le lecteur d'écran
    // repartent du nouveau contenu, pas du lien cliqué dans le menu.
    router.events.pipe(
      filter(evenement => evenement instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => this.contenu?.nativeElement.focus({ preventScroll: true }));
  }

  /** Lien d'évitement : place le focus sur le contenu sans modifier l'URL. */
  allerAuContenu(evenement: Event): void {
    evenement.preventDefault();
    this.contenu?.nativeElement.focus();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
