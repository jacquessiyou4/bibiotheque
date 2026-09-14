import { Component } from '@angular/core';
import { ThemeService } from './_service/theme.service';
import { NotificationService } from './_service/notification.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Library Management System';

  constructor(private themeService: ThemeService,
              public notificationService: NotificationService) {
    this.themeService.init();
  }
}
