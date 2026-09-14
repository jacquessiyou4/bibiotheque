import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ThemeService } from './_service/theme.service';
import { NotificationService } from './_service/notification.service';

// OnPush : les notifications passent par le pipe async, qui marque la vue
// à chaque nouvelle valeur ; le routeur marque la vue à chaque activation.
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  title = 'Library Management System';

  constructor(private themeService: ThemeService,
              public notificationService: NotificationService) {
    this.themeService.init();
  }
}
