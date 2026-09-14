import { apiUrl } from './api-config';

describe('api-config', () => {
  const envInitial = (window as any).__env;

  afterEach(() => {
    (window as any).__env = envInitial;
  });

  it('apiUrl renvoie la valeur par défaut en l’absence de window.__env', () => {
    (window as any).__env = undefined;
    expect(apiUrl()).toBe('http://localhost:8080');
  });

  it('apiUrl renvoie la valeur injectée par le runtime (docker-entrypoint)', () => {
    (window as any).__env = { apiUrl: 'http://backend:8080' };
    expect(apiUrl()).toBe('http://backend:8080');
  });

  it('apiUrl ignore une ancienne configuration Keycloak encore présente dans env.js', () => {
    (window as any).__env = { apiUrl: 'http://backend:8080', keycloakUrl: 'http://keycloak:8080' };
    expect(apiUrl()).toBe('http://backend:8080');
  });
});
