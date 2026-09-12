import { ErrorHandler, Injectable } from '@angular/core';

/**
 * Gestionnaire d'erreurs global pour l'application.
 * Capture toutes les erreurs non gérées et les journalise proprement.
 * En production, on pourrait intégrer un service de notification (toast)
 * ou un outil de monitoring (Sentry, etc.).
 */
@Injectable()
export class AppErrorHandler implements ErrorHandler {

  handleError(error: any): void {
    // Extraire un message lisible
    const message = error?.message || error?.statusText || String(error);
    const status = error?.status;

    if (status === 401) {
      console.error('[Auth] Session expirée ou token invalide');
    } else if (status === 403) {
      console.error('[Auth] Accès interdit');
    } else if (status === 0) {
      console.error('[Réseau] Impossible de joindre le serveur');
    } else {
      console.error('[Erreur]', message);
    }
  }
}
