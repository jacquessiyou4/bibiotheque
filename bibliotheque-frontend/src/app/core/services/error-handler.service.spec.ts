import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { AppErrorHandler } from './error-handler.service';
import { NotificationService } from './notification.service';

describe('AppErrorHandler', () => {
  let handler: AppErrorHandler;
  let notificationSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);
    TestBed.configureTestingModule({
      providers: [
        AppErrorHandler,
        { provide: NotificationService, useValue: notificationSpy },
      ],
    });
    handler = TestBed.inject(AppErrorHandler);
    // Évite de polluer la sortie des tests ; permet aussi de vérifier la trace.
    spyOn(console, 'error');
  });

  it('garde toujours la trace de l’erreur dans la console', () => {
    const erreur = new Error('bug inattendu');

    handler.handleError(erreur);

    expect(console.error).toHaveBeenCalledWith(erreur);
  });

  it('401 : indique que la session a expiré', () => {
    handler.handleError(new HttpErrorResponse({ status: 401 }));

    expect(notificationSpy.showError).toHaveBeenCalledWith('Session expirée. Veuillez vous reconnecter.');
  });

  it('403 : indique un accès interdit', () => {
    handler.handleError(new HttpErrorResponse({ status: 403 }));

    expect(notificationSpy.showError).toHaveBeenCalledWith('Accès interdit.');
  });

  it('statut 0 : indique que le serveur est injoignable', () => {
    handler.handleError(new HttpErrorResponse({ status: 0 }));

    expect(notificationSpy.showError).toHaveBeenCalledWith('Impossible de joindre le serveur.');
  });

  it('erreur serveur 500 : affiche le message générique', () => {
    handler.handleError(new HttpErrorResponse({ status: 500 }));

    expect(notificationSpy.showError).toHaveBeenCalledWith('Une erreur est survenue. Réessayez.');
  });

  it('erreur JavaScript sans statut : affiche le message générique', () => {
    handler.handleError(new TypeError('Cannot read properties of undefined'));

    expect(notificationSpy.showError).toHaveBeenCalledWith('Une erreur est survenue. Réessayez.');
  });

  it('erreur nulle : ne plante pas et affiche le message générique', () => {
    expect(() => handler.handleError(null)).not.toThrow();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Une erreur est survenue. Réessayez.');
  });
});
