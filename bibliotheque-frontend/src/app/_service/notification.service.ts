import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() { }

  showSuccess(message: string): void {
    // TODO: Replace with a toast library (e.g. ngx-toastr)
  }

  showError(message: string): void {
    // TODO: Replace with a toast library (e.g. ngx-toastr)
  }

  showWarning(message: string): void {
    // TODO: Replace with a toast library (e.g. ngx-toastr)
  }

  showInfo(message: string): void {
    // TODO: Replace with a toast library (e.g. ngx-toastr)
  }
}
