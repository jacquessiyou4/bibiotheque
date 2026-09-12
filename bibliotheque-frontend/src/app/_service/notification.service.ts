import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() { }

  showSuccess(message: string): void {
    // In a real app, use a toast library like ngx-toastr
    console.log('[SUCCESS]', message);
  }

  showError(message: string): void {
    // In a real app, use a toast library like ngx-toastr
    console.error('[ERROR]', message);
  }

  showWarning(message: string): void {
    // In a real app, use a toast library like ngx-toastr
    console.warn('[WARNING]', message);
  }

  showInfo(message: string): void {
    // In a real app, use a toast library like ngx-toastr
    console.log('[INFO]', message);
  }
}
