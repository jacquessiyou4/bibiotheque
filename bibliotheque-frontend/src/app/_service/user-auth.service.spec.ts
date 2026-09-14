import { TestBed } from '@angular/core/testing';

import { UserAuthService } from './user-auth.service';

describe('UserAuthService', () => {
  let service: UserAuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserAuthService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('setToken/getToken mémorise le jeton dans localStorage', () => {
    expect(service.getToken()).toBeNull();

    service.setToken('jeton');

    expect(service.getToken()).toBe('jeton');
    expect(localStorage.getItem('jwtToken')).toBe('jeton');
  });

  it('setRoles/getRoles mémorise les rôles au format [{roleName}]', () => {
    service.setRoles([{ roleName: 'ADHERENT' }]);

    const roles = service.getRoles();
    expect(roles).not.toBeNull();
    expect(roles!.length).toBe(1);
    expect(roles![0].roleName).toBe('ADHERENT');
    expect(JSON.parse(localStorage.getItem('roles') || '[]')).toEqual([{ roleName: 'ADHERENT' }]);
  });

  it('setUserId/getUserId mémorise l\u2019identifiant local', () => {
    service.setUserId(7);

    expect(service.getUserId()).toBe(7);
  });

  it('sans session, getUserId et getName valent null', () => {
    // JSON.parse(localStorage.getItem(...)!) sur une clé absente renvoie null.
    expect(service.getUserId()).toBeNull();
    expect(service.getName()).toBeNull();
  });

  it('clear() efface token, rôles, nom et userId', () => {
    service.setToken('jeton');
    service.setRoles([{ roleName: 'ADHERENT' }]);
    service.setName('Nom');
    service.setUserId(1);

    service.clear();

    expect(service.getToken()).toBeNull();
    expect(service.getRoles()).toBeNull();
    expect(service.getName()).toBeNull();
    expect(service.getUserId()).toBeNull();
    expect(service.isLoggedIn()).toBeFalsy();
  });

  it('isLoggedIn exige un jeton ET des rôles', () => {
    service.setToken('jeton');
    expect(service.isLoggedIn()).toBeFalsy();

    service.setRoles([{ roleName: 'ADHERENT' }]);
    expect(service.isLoggedIn()).toBeTruthy();
  });

  it('decodeJwt décode la charge utile base64url du jeton', () => {
    const chargeUtile = { realm_access: { roles: ['Admin'] }, preferred_username: 'admin' };
    const jeton = btoa('en-tête') + '.' + btoa(JSON.stringify(chargeUtile)) + '.signature';

    const decode = service.decodeJwt(jeton);

    expect(decode.preferred_username).toBe('admin');
    expect(decode.realm_access.roles).toEqual(['Admin']);
  });
});

