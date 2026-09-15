import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let prefereClair: boolean;

  beforeEach(() => {
    localStorage.clear();
    // Préférence du système simulée : le navigateur de test n'en impose pas.
    prefereClair = false;
    spyOn(window, 'matchMedia').and.callFake((requete: string) =>
      ({ matches: requete.includes('light') && prefereClair } as MediaQueryList));
    service = TestBed.inject(ThemeService);
    document.documentElement.removeAttribute('data-theme');
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('sans choix mémorisé ni préférence claire du système, le thème est sombre', () => {
    expect(service.getTheme()).toBe('dark');
  });

  it('sans choix mémorisé, suit la préférence claire du système', () => {
    prefereClair = true;

    expect(service.getTheme()).toBe('light');
  });

  it('un choix mémorisé l’emporte sur la préférence du système', () => {
    prefereClair = true;
    localStorage.setItem('theme', 'dark');

    expect(service.getTheme()).toBe('dark');
  });

  it('setTheme mémorise le thème et applique l’attribut data-theme', () => {
    service.setTheme('light');

    expect(localStorage.getItem('theme')).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('setTheme dark retire l’attribut data-theme du document', () => {
    service.setTheme('light');

    service.setTheme('dark');

    expect(document.documentElement.hasAttribute('data-theme')).toBeFalse();
  });

  it('toggleTheme passe du sombre au clair puis retourne au sombre', () => {
    service.toggleTheme();
    expect(service.getTheme()).toBe('light');

    service.toggleTheme();
    expect(service.getTheme()).toBe('dark');
  });

  it('init applique le thème mémorisé d’une session précédente', () => {
    localStorage.setItem('theme', 'light');

    service.init();

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
