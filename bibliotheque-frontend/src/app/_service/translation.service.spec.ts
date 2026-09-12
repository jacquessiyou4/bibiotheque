import { TestBed } from '@angular/core/testing';

import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.clear();
    service = TestBed.inject(TranslationService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('la langue par défaut est l\u2019anglais', () => {
    expect(service.getLang()).toBe('en');
  });

  it('setLang mémorise la langue choisie', () => {
    service.setLang('fr');

    expect(service.getLang()).toBe('fr');
    expect(localStorage.getItem('lang')).toBe('fr');
  });

  it('toggleLang bascule entre anglais et français', () => {
    service.toggleLang();
    expect(service.getLang()).toBe('fr');

    service.toggleLang();
    expect(service.getLang()).toBe('en');
  });

  it('translate renvoie la traduction de la langue courante', () => {
    service.setLang('fr');
    expect(service.translate('nav.login')).toBe('Connexion');

    service.setLang('en');
    expect(service.translate('nav.login')).toBe('Login');
  });

  it('translate renvoie la clé telle quelle si elle est inconnue', () => {
    expect(service.translate('cle.inexistante')).toBe('cle.inexistante');
  });

  it('translate remplace les paramètres {nom} dans le texte', () => {
    service.setLang('en');

    const rendu = service.translate('reservations.confirmDelete', { book: 'L1' });

    expect(rendu).toContain('L1');
    expect(rendu).not.toContain('{book}');
  });
});
