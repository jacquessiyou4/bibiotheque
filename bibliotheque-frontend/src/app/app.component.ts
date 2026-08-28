import { Component } from '@angular/core';
import { ThemeService } from './_service/theme.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Library Management System';

  constructor(private themeService: ThemeService) {
    this.themeService.init();
  }
}
