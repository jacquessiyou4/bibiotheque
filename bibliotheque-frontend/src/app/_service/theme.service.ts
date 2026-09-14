import { Injectable } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'theme';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {

  getTheme(): Theme {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'light' ? 'light' : 'dark';
    } catch {
      return 'dark';
    }
  }

  setTheme(theme: Theme): void {
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // localStorage indisponible (navigation privée, etc.) : le thème
      // reste appliqué pour la session en cours, simplement pas mémorisé.
    }
    this.apply(theme);
  }

  toggleTheme(): void {
    this.setTheme(this.getTheme() === 'dark' ? 'light' : 'dark');
  }

  init(): void {
    this.apply(this.getTheme());
  }

  private apply(theme: Theme): void {
    if (theme === 'light') {
      document.documentElement.setAttribute('data-theme', 'light');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  }
}
