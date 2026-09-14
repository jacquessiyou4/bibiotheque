import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    service = TestBed.inject(ThemeService);
    document.documentElement.removeAttribute('data-theme');
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('le thème par défaut est sombre', () => {
    expect(service.getTheme()).toBe('dark');
  });

  it('setTheme mémorise le thème et applique l\u2019attribut data-theme', () => {
    service.setTheme('light');

    expect(localStorage.getItem('theme')).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('setTheme dark retire l\u2019attribut data-theme du document', () => {
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

  it('init applique le thème mémorisé d\u2019une session précédente', () => {
    localStorage.setItem('theme', 'light');

    service.init();

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
