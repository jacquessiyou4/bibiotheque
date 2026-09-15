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

  it('sans choix mémorisé, un navigateur anglophone affiche l\u2019anglais', () => {
    spyOnProperty(navigator, 'language').and.returnValue('en-US');
    expect(service.getLang()).toBe('en');
  });

  it('sans choix mémorisé, un navigateur francophone affiche le français', () => {
    spyOnProperty(navigator, 'language').and.returnValue('fr-CA');
    expect(service.getLang()).toBe('fr');
  });

  it('setLang met à jour <html lang> et le titre de la page', () => {
    service.setLang('fr');
    expect(document.documentElement.lang).toBe('fr');
    expect(document.title).toBe('Bibliothèque');

    service.setLang('en');
    expect(document.documentElement.lang).toBe('en');
    expect(document.title).toBe('Library');
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
