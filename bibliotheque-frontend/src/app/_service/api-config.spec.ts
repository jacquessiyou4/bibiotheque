import {
  apiUrl,
  keycloakClient,
  keycloakRealm,
  keycloakUrl
} from './api-config';

describe('api-config', () => {
  const envInitial = (window as any).__env;

  afterEach(() => {
    (window as any).__env = envInitial;
  });

  it('apiUrl renvoie la valeur par défaut en l\u2019absence de window.__env', () => {
    (window as any).__env = undefined;
    expect(apiUrl()).toBe('http://localhost:8080');
  });

  it('apiUrl renvoie la valeur injectée par le runtime (docker-entrypoint)', () => {
    (window as any).__env = { apiUrl: 'http://backend:8080' };
    expect(apiUrl()).toBe('http://backend:8080');
  });

  it('keycloakUrl renvoie la valeur injectée ou localhost:8081', () => {
    (window as any).__env = undefined;
    expect(keycloakUrl()).toBe('http://localhost:8081');

    (window as any).__env = { keycloakUrl: 'http://keycloak:8080' };
    expect(keycloakUrl()).toBe('http://keycloak:8080');
  });

  it('keycloakRealm renvoie la valeur injectée ou bibliotheque', () => {
    (window as any).__env = undefined;
    expect(keycloakRealm()).toBe('bibliotheque');

    (window as any).__env = { keycloakRealm: 'autre-realm' };
    expect(keycloakRealm()).toBe('autre-realm');
  });

  it('keycloakClient est fixé par le code (client public du realm)', () => {
    expect(keycloakClient()).toBe('bibliotheque-frontend');
  });
});
