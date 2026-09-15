import { UrlMatcher, UrlSegment } from '@angular/router';

/**
 * Matcher de route pour charger une fonctionnalité à la demande sans changer
 * ses URL : la route correspond quand le premier segment fait partie des
 * écrans de la fonctionnalité (« books », « create-book »…), mais ne consomme
 * aucun segment, pour que les routes enfants du module reçoivent l'URL complète.
 *
 * Angular 14.0 ne propose pas encore canMatch : ce matcher en tient lieu.
 */
export function premierSegmentParmi(chemins: string[]): UrlMatcher {
  return (segments: UrlSegment[]) =>
    segments.length > 0 && chemins.includes(segments[0].path) ? { consumed: [] } : null;
}
