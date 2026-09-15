import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-forbidden',
  templateUrl: './forbidden.component.html',
  styleUrls: ['./forbidden.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ForbiddenComponent {

  constructor() { }


}
