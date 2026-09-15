import { ChangeDetectorRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { TranslatePipe } from './translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { Lang } from '../../core/i18n/translations';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;
  let cdrSpy: jasmine.SpyObj<ChangeDetectorRef>;
  let langue$: BehaviorSubject<Lang>;

  beforeEach(() => {
    langue$ = new BehaviorSubject<Lang>('en');
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['translate'], { lang$: langue$ });
    cdrSpy = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
    pipe = new TranslatePipe(translationServiceSpy, cdrSpy);
  });

  afterEach(() => pipe.ngOnDestroy());

  it('délègue la traduction au service', () => {
    translationServiceSpy.translate.and.returnValue('Book List');

    expect(pipe.transform('books.title')).toBe('Book List');
    expect(translationServiceSpy.translate).toHaveBeenCalledWith('books.title', undefined);
  });

  it('transmet les paramètres de substitution au service', () => {
    pipe.transform('reservations.confirmDelete', { book: 'L1' });

    expect(translationServiceSpy.translate).toHaveBeenCalledWith('reservations.confirmDelete', { book: 'L1' });
  });

  it('marque la vue OnPush à chaque changement de langue, pas à la création', () => {
    expect(cdrSpy.markForCheck).not.toHaveBeenCalled();

    langue$.next('fr');

    expect(cdrSpy.markForCheck).toHaveBeenCalledTimes(1);
  });

  it('ne marque plus la vue une fois détruit', () => {
    pipe.ngOnDestroy();

    langue$.next('fr');

    expect(cdrSpy.markForCheck).not.toHaveBeenCalled();
  });

  it('fonctionne avec une doublure de service sans lang$', () => {
    const sansFlux = new TranslatePipe({ translate: (cle: string) => cle } as TranslationService, cdrSpy);

    expect(sansFlux.transform('books.title')).toBe('books.title');
    expect(() => sansFlux.ngOnDestroy()).not.toThrow();
  });
});
