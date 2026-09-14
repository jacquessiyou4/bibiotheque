import { TranslatePipe } from './translate.pipe';
import { TranslationService } from '../_service/translation.service';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;

  beforeEach(() => {
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['translate']);
    pipe = new TranslatePipe(translationServiceSpy);
  });

  it('délègue la traduction au service', () => {
    translationServiceSpy.translate.and.returnValue('Book List');

    expect(pipe.transform('books.title')).toBe('Book List');
    expect(translationServiceSpy.translate).toHaveBeenCalledWith('books.title', undefined);
  });

  it('transmet les paramètres de substitution au service', () => {
    pipe.transform('reservations.confirmDelete', { book: 'L1' });

    expect(translationServiceSpy.translate).toHaveBeenCalledWith('reservations.confirmDelete', { book: 'L1' });
  });
});
