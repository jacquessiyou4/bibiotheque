import { TestBed } from '@angular/core/testing';

import { UserAuthService } from './user-auth.service';

/** Jeton non signé dont seule la charge utile compte pour le frontend. */
function jeton(chargeUtile: object): string {
  const base64Url = (texte: string) => btoa(texte).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return base64Url('{"alg":"none"}') + '.' + base64Url(JSON.stringify(chargeUtile)) + '.signature';
}

describe('UserAuthService', () => {
  let service: UserAuthService;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserAuthService);
  });

  afterEach(() => {
    service.clear();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('setToken/getToken garde le jeton dans sessionStorage, jamais dans localStorage', () => {
    expect(service.getToken()).toBeNull();

    service.setToken('jeton');

    expect(service.getToken()).toBe('jeton');
    expect(JSON.parse(sessionStorage.getItem('session')!).token).toBe('jeton');
    expect(localStorage.getItem('jwtToken')).toBeNull();
  });

  it('les rôles viennent du jeton quand il porte realm_access', () => {
    service.setRoles([{ roleName: 'Admin' }]);
    service.setToken(jeton({ realm_access: { roles: ['User', 'ADHERENT'] } }));

    expect(service.getRoles()).toEqual([{ roleName: 'User' }, { roleName: 'ADHERENT' }]);
  });

  it('sans rôles dans le jeton, setRoles/getRoles conserve les rôles posés', () => {
    service.setRoles([{ roleName: 'ADHERENT' }]);

    expect(service.getRoles()).toEqual([{ roleName: 'ADHERENT' }]);
  });

  it('setUserId/setName mémorisent l’identifiant local et le nom', () => {
    service.setUserId(7);
    service.setName('Nom');

    expect(service.getUserId()).toBe(7);
    expect(service.getName()).toBe('Nom');
  });

  it('sans session, getUserId, getName et getRoles valent null', () => {
    expect(service.getUserId()).toBeNull();
    expect(service.getName()).toBeNull();
    expect(service.getRoles()).toBeNull();
  });

  it('clear() ferme la session mais garde le thème et la langue', () => {
    localStorage.setItem('theme', 'light');
    localStorage.setItem('lang', 'fr');
    service.setToken('jeton');
    service.setRoles([{ roleName: 'ADHERENT' }]);
    service.setName('Nom');
    service.setUserId(1);

    service.clear();

    expect(service.getToken()).toBeNull();
    expect(service.getRoles()).toBeNull();
    expect(service.getName()).toBeNull();
    expect(service.getUserId()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(sessionStorage.getItem('session')).toBeNull();
    expect(localStorage.getItem('theme')).toBe('light');
    expect(localStorage.getItem('lang')).toBe('fr');
  });

  it('isLoggedIn exige un jeton ET des rôles', () => {
    service.setToken('jeton');
    expect(service.isLoggedIn()).toBeFalse();

    service.setRoles([{ roleName: 'ADHERENT' }]);
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('un jeton déjà expiré n’est jamais rendu et ferme la session', () => {
    let expirations = 0;
    service.sessionExpiree$.subscribe(() => expirations++);
    const passe = Math.floor(Date.now() / 1000) - 60;

    service.setToken(jeton({ exp: passe, realm_access: { roles: ['User'] } }));

    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(expirations).toBe(1);
  });

  it('la session se ferme d’elle-même à l’expiration du jeton', () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 8, 15, 10, 0, 0));
    try {
      let expirations = 0;
      service.sessionExpiree$.subscribe(() => expirations++);
      const dansCinqMinutes = Math.floor(Date.now() / 1000) + 300;
      service.setToken(jeton({ exp: dansCinqMinutes, realm_access: { roles: ['User'] } }));

      jasmine.clock().tick(299_000);
      expect(service.isLoggedIn()).toBeTrue();

      jasmine.clock().tick(2_000);
      expect(expirations).toBe(1);
      expect(service.getToken()).toBeNull();
    } finally {
      jasmine.clock().uninstall();
    }
  });

  it('au démarrage, restaure la session de l’onglet et supprime les anciennes clés localStorage', () => {
    localStorage.setItem('jwtToken', 'ancien-jeton');
    sessionStorage.setItem('session', JSON.stringify(
      { token: 'jeton', roles: [{ roleName: 'User' }], userId: 3, name: 'Nom', expireA: null }));

    const autre = new UserAuthService();

    expect(autre.getToken()).toBe('jeton');
    expect(autre.getUserId()).toBe(3);
    expect(localStorage.getItem('jwtToken')).toBeNull();
    autre.clear();
  });

  it('session$ publie chaque changement de session', () => {
    const valeurs: (string | null)[] = [];
    service.session$.subscribe(session => valeurs.push(session?.name ?? null));

    service.setName('Nom');
    service.clear();

    expect(valeurs).toEqual([null, 'Nom', null]);
  });

  it('decodeJwt décode la charge utile base64url du jeton', () => {
    const decode = service.decodeJwt(jeton({ realm_access: { roles: ['Admin'] }, preferred_username: 'admin' }));

    expect(decode.preferred_username).toBe('admin');
    expect(decode.realm_access.roles).toEqual(['Admin']);
  });

  it('decodeJwt renvoie un objet vide pour un jeton illisible', () => {
    expect(service.decodeJwt('pas-un-jwt')).toEqual({});
  });
});
