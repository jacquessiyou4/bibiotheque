import { HttpErrorResponse } from '@angular/common/http';

/**
 * Corps d'erreur renvoyé par le backend (RFC 7807, application/problem+json).
 * « message » reste présent pour les clients écrits avant « detail ».
 */
export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  message?: string;
  code?: string;
  requestId?: string;
  errors?: Record<string, string>;
}

/**
 * Texte à montrer à l'utilisateur pour une erreur HTTP : le message du
 * backend quand il existe, suivi d'une référence courte (début du
 * X-Request-ID) à communiquer au support pour retrouver la requête dans les logs.
 */
export function messageErreur(err: unknown, repli: string): string {
  const http = err instanceof HttpErrorResponse ? err : null;
  if (http?.status === 0) {
    return "Le serveur est injoignable. Vérifiez qu'il est démarré, puis réessayez.";
  }
  const corps = (err as { error?: unknown } | null)?.error;
  if (typeof corps === 'string' && corps) {
    return corps;
  }
  if (corps && typeof corps === 'object') {
    const probleme = corps as ApiProblem;
    const texte = probleme.detail || probleme.message || repli;
    return probleme.requestId ? `${texte} (réf. ${probleme.requestId.slice(0, 8)})` : texte;
  }
  return repli;
}
