import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type NotificationType = 'success' | 'danger' | 'warning' | 'info';

export interface Notification {
  id: number;
  type: NotificationType;
  message: string;
}

const DUREE_AFFICHAGE_MS = 4000;

/**
 * Notifications affichées en haut à droite de l'écran (voir
 * app.component.html). Chaque message disparaît seul après quelques
 * secondes, ou au clic sur sa croix.
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private nextId = 1;
  private readonly notificationsSubject = new BehaviorSubject<Notification[]>([]);
  readonly notifications$ = this.notificationsSubject.asObservable();

  showSuccess(message: string): void {
    this.push('success', message);
  }

  showError(message: string): void {
    this.push('danger', message);
  }

  showWarning(message: string): void {
    this.push('warning', message);
  }

  showInfo(message: string): void {
    this.push('info', message);
  }

  dismiss(id: number): void {
    this.notificationsSubject.next(this.notificationsSubject.value.filter(n => n.id !== id));
  }

  private push(type: NotificationType, message: string): void {
    const notification: Notification = { id: this.nextId++, type, message };
    this.notificationsSubject.next([...this.notificationsSubject.value, notification]);
    setTimeout(() => this.dismiss(notification.id), DUREE_AFFICHAGE_MS);
  }
}
