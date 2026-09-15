import { Injectable } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'theme';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {

  /**
   * Thème choisi par l'utilisateur ; à la première visite, celui du système
   * (prefers-color-scheme), et sombre si le navigateur ne l'indique pas.
   */
  getTheme(): Theme {
    try {
      const memorise = localStorage.getItem(STORAGE_KEY);
      if (memorise === 'light' || memorise === 'dark') {
        return memorise;
      }
    } catch {
      // localStorage indisponible : on se replie sur la préférence du système.
    }
    return ThemeService.themeDuSysteme();
  }

  private static themeDuSysteme(): Theme {
    const prefereClair = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      && window.matchMedia('(prefers-color-scheme: light)').matches;
    return prefereClair ? 'light' : 'dark';
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
