import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Lang, TRANSLATIONS } from '../i18n/translations';

const STORAGE_KEY = 'lang';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {

  private readonly langSubject = new BehaviorSubject<Lang>(this.getLang());

  /**
   * Émet la langue à chaque changement. Les composants sont en OnPush : le
   * pipe translate s'y abonne pour redessiner leur vue quand la langue change.
   */
  readonly lang$: Observable<Lang> = this.langSubject.asObservable();

  constructor() {
    this.appliquerAuDocument(this.getLang());
  }

  /**
   * Langue choisie par l'utilisateur ; à la première visite, celle du
   * navigateur (français si navigator.language commence par « fr »).
   */
  getLang(): Lang {
    try {
      const memorisee = localStorage.getItem(STORAGE_KEY);
      if (memorisee === 'fr' || memorisee === 'en') {
        return memorisee;
      }
    } catch {
      // localStorage indisponible : on se replie sur la langue du navigateur.
    }
    return TranslationService.langueDuNavigateur();
  }

  setLang(lang: Lang): void {
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      // localStorage indisponible : la langue reste appliquée pour la
      // session en cours, simplement pas mémorisée.
    }
    this.appliquerAuDocument(lang);
    this.langSubject.next(lang);
  }

  toggleLang(): void {
    this.setLang(this.getLang() === 'en' ? 'fr' : 'en');
  }

  translate(key: string, params?: Record<string, string>): string {
    const dict = TRANSLATIONS[this.getLang()];
    let value = dict[key] ?? key;
    if (params) {
      Object.keys(params).forEach(p => {
        value = value.replace(`{${p}}`, params[p]);
      });
    }
    return value;
  }

  /**
   * <html lang> suit la langue affichée : les lecteurs d'écran prononcent le
   * texte avec la bonne voix, le navigateur propose la bonne correction.
   */
  private appliquerAuDocument(lang: Lang): void {
    document.documentElement.lang = lang;
    document.title = TRANSLATIONS[lang]['app.title'] ?? document.title;
  }

  private static langueDuNavigateur(): Lang {
    const langue = typeof navigator !== 'undefined' ? (navigator.language || '') : '';
    return langue.toLowerCase().startsWith('fr') ? 'fr' : 'en';
  }
}
