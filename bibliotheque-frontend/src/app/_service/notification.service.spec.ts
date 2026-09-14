import { fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { Notification, NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let affichees: Notification[] = [];

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
    service.notifications$.subscribe(n => affichees = n);
  });

  it('ne contient aucune notification au démarrage', () => {
    expect(affichees).toEqual([]);
  });

  it('showSuccess, showError, showWarning et showInfo ajoutent un message du bon style', fakeAsync(() => {
    service.showSuccess('Emprunt réussi');
    service.showError('Erreur lors du retour');
    service.showWarning('Votre session a expiré');
    service.showInfo('Information');

    expect(affichees.map(n => [n.type, n.message])).toEqual([
      ['success', 'Emprunt réussi'],
      ['danger', 'Erreur lors du retour'],
      ['warning', 'Votre session a expiré'],
      ['info', 'Information'],
    ]);
    flush();
  }));

  it('attribue un identifiant différent à chaque notification', fakeAsync(() => {
    service.showInfo('un');
    service.showInfo('deux');

    expect(affichees[0].id).not.toBe(affichees[1].id);
    flush();
  }));

  it('dismiss retire uniquement la notification ciblée', fakeAsync(() => {
    service.showInfo('garder');
    service.showError('fermer');

    service.dismiss(affichees[1].id);

    expect(affichees.map(n => n.message)).toEqual(['garder']);
    flush();
  }));

  it('dismiss d’un identifiant inconnu ne change rien', fakeAsync(() => {
    service.showInfo('garder');

    service.dismiss(-1);

    expect(affichees.length).toBe(1);
    flush();
  }));

  it('une notification reste affichée 10 secondes puis disparaît seule', fakeAsync(() => {
    service.showWarning('Jeton invalide ou expiré. Veuillez vous reconnecter.');

    tick(9999);
    expect(affichees.length).toBe(1);

    tick(1);
    expect(affichees.length).toBe(0);
  }));

  it('chaque notification disparaît 10 secondes après sa propre apparition', fakeAsync(() => {
    service.showInfo('première');
    tick(4000);
    service.showInfo('seconde');

    tick(6000);
    expect(affichees.map(n => n.message)).toEqual(['seconde']);

    tick(4000);
    expect(affichees).toEqual([]);
  }));
});
