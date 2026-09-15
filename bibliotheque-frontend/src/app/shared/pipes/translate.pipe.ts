import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Subscription } from 'rxjs';
import { skip } from 'rxjs/operators';
import { TranslationService } from '../../core/services/translation.service';

@Pipe({
  name: 'translate',
  pure: false // doit se réévaluer quand la langue change, pas seulement quand la clé change
})
export class TranslatePipe implements PipeTransform, OnDestroy {

  private readonly abonnement?: Subscription;

  constructor(private translationService: TranslationService, cdr: ChangeDetectorRef) {
    // Composants en OnPush : un changement de langue (clic dans le header) ne
    // marque pas leur vue, pure: false ne suffit donc plus. Le pipe marque la
    // vue qui l'utilise à chaque changement. lang$ est absent des doublures de
    // TranslationService utilisées dans les tests des composants.
    this.abonnement = translationService.lang$?.pipe(skip(1)).subscribe(() => cdr.markForCheck());
  }

  transform(key: string, params?: Record<string, string>): string {
    return this.translationService.translate(key, params);
  }

  ngOnDestroy(): void {
    this.abonnement?.unsubscribe();
  }
}
