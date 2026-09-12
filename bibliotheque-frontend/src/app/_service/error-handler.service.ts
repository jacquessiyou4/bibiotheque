import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { NotificationService } from './notification.service';

/**
 * Gestionnaire d'erreurs global pour l'application.
 * Capture toutes les erreurs non gérées et les affiche via le service de notification.
 */
@Injectable()
export class AppErrorHandler implements ErrorHandler {
  constructor(private injector: Injector) {}

  handleError(error: any): void {
    const notification = this.injector.get(NotificationService);
    const status = error?.status;

    if (status === 401) {
      notification.showError('Session expirée. Veuillez vous reconnecter.');
    } else if (status === 403) {
      notification.showError('Accès interdit.');
    } else if (status === 0) {
      notification.showError('Impossible de joindre le serveur.');
    } else {
      notification.showError('Une erreur est survenue. Réessayez.');
    }
  }
}
