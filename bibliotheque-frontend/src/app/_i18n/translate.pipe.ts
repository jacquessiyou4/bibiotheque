import { Pipe, PipeTransform } from '@angular/core';
import { TranslationService } from '../_service/translation.service';

@Pipe({
  name: 'translate',
  pure: false // doit se réévaluer quand la langue change, pas seulement quand la clé change
})
export class TranslatePipe implements PipeTransform {

  constructor(private translationService: TranslationService) { }

  transform(key: string, params?: Record<string, string>): string {
    return this.translationService.translate(key, params);
  }
}
