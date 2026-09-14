import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Lang, TRANSLATIONS } from '../_i18n/translations';

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

  getLang(): Lang {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'fr' ? 'fr' : 'en';
    } catch {
      return 'en';
    }
  }

  setLang(lang: Lang): void {
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      // localStorage indisponible : la langue reste appliquée pour la
      // session en cours, simplement pas mémorisée.
    }
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
}
