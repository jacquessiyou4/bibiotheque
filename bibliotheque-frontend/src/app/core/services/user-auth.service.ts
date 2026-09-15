import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

/** Session de l'onglet courant. */
export interface Session {
  token: string | null;
  // Rôles posés explicitement ; ignorés dès que le jeton porte realm_access.
  roles: { roleName: string }[] | null;
  userId: number | null;
  name: string | null;
  // Instant d'expiration du jeton (claim exp, en ms), null si inconnu.
  expireA: number | null;
}

const CLE_SESSION = 'session';
// Anciennes clés localStorage : le jeton y survivait à la fermeture du
// navigateur et restait lisible par tout script de la page.
const ANCIENNES_CLES = ['jwtToken', 'roles', 'userId', 'name'];
// setTimeout n'accepte pas de délai au-delà de 2^31 - 1 ms (~24,8 jours).
const DELAI_MAX_MS = 2147483647;

/**
 * Session de l'utilisateur connecté, source unique de vérité :
 * - le jeton est gardé en mémoire et dans sessionStorage (limité à l'onglet,
 *   effacé à sa fermeture), plus dans localStorage ;
 * - les rôles sont lus dans le jeton (ceux que vérifie le backend) ;
 * - à l'instant exp du jeton, la session se ferme d'elle-même et
 *   sessionExpiree$ émet (AppComponent redirige vers la connexion).
 */
@Injectable({
  providedIn: 'root'
})
export class UserAuthService {

  private readonly sessionSubject = new BehaviorSubject<Session | null>(this.restaurer());
  readonly session$: Observable<Session | null> = this.sessionSubject.asObservable();

  private readonly expirationSubject = new Subject<void>();
  readonly sessionExpiree$: Observable<void> = this.expirationSubject.asObservable();

  private minuteur: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.supprimerAnciennesCles();
    this.programmerExpiration(this.sessionSubject.value);
  }

  public setRoles(roles: { roleName: string }[]) {
    this.mettreAJour({ roles });
  }

  public getRoles(): { roleName: string }[] | null {
    const session = this.sessionSubject.value;
    if (!session) {
      return null;
    }
    const rolesDuJeton = session.token ? this.decodeJwt(session.token)?.realm_access?.roles : undefined;
    if (Array.isArray(rolesDuJeton)) {
      return rolesDuJeton.map((roleName: string) => ({ roleName }));
    }
    return session.roles;
  }

  public setToken(jwtToken: string) {
    const exp = this.decodeJwt(jwtToken)?.exp;
    this.mettreAJour({ token: jwtToken, expireA: typeof exp === 'number' ? exp * 1000 : null });
  }

  /** Jeton courant, ou null s'il est absent ou expiré (la session est alors fermée). */
  public getToken(): string | null {
    const session = this.sessionSubject.value;
    if (!session?.token) {
      return null;
    }
    if (session.expireA !== null && Date.now() >= session.expireA) {
      this.expirer();
      return null;
    }
    return session.token;
  }

  public setUserId(userId: number) {
    this.mettreAJour({ userId });
  }

  public getUserId(): number | null {
    return this.sessionSubject.value?.userId ?? null;
  }

  public setName(name: string) {
    this.mettreAJour({ name });
  }

  public getName(): string | null {
    return this.sessionSubject.value?.name ?? null;
  }

  /** Ferme la session. Les préférences (thème, langue) sont conservées. */
  public clear() {
    this.arreterMinuteur();
    try {
      sessionStorage.removeItem(CLE_SESSION);
    } catch {
      // sessionStorage indisponible : la session en mémoire est tout de même vidée.
    }
    this.supprimerAnciennesCles();
    this.sessionSubject.next(null);
  }

  /**
   * Décode la partie « payload » d'un JWT (sans en vérifier la signature :
   * la validation est faite par le backend et Keycloak).
   */
  public decodeJwt(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''));
      return JSON.parse(jsonPayload);
    } catch (e) {
      return {};
    }
  }

  public isLoggedIn() {
    const roles = this.getRoles();
    return !!this.getToken() && !!roles && roles.length > 0;
  }

  private mettreAJour(changements: Partial<Session>): void {
    const actuelle: Session = this.sessionSubject.value
      ?? { token: null, roles: null, userId: null, name: null, expireA: null };
    const session: Session = { ...actuelle, ...changements };
    try {
      sessionStorage.setItem(CLE_SESSION, JSON.stringify(session));
    } catch {
      // Navigation privée stricte : la session reste valable pour la page ouverte.
    }
    this.sessionSubject.next(session);
    this.programmerExpiration(session);
  }

  private programmerExpiration(session: Session | null): void {
    this.arreterMinuteur();
    if (!session?.token || session.expireA === null) {
      return;
    }
    const delai = session.expireA - Date.now();
    this.minuteur = setTimeout(() => this.expirer(), Math.max(0, Math.min(delai, DELAI_MAX_MS)));
  }

  private expirer(): void {
    if (!this.sessionSubject.value) {
      return;
    }
    this.clear();
    this.expirationSubject.next();
  }

  private arreterMinuteur(): void {
    if (this.minuteur !== null) {
      clearTimeout(this.minuteur);
      this.minuteur = null;
    }
  }

  private restaurer(): Session | null {
    try {
      const brut = sessionStorage.getItem(CLE_SESSION);
      return brut ? JSON.parse(brut) as Session : null;
    } catch {
      return null;
    }
  }

  private supprimerAnciennesCles(): void {
    try {
      ANCIENNES_CLES.forEach(cle => localStorage.removeItem(cle));
    } catch {
      // localStorage indisponible : rien à nettoyer.
    }
  }
}
