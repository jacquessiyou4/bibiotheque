declare global {
  interface Window {
    __env?: {
      apiUrl?: string;
      keycloakUrl?: string;
      keycloakRealm?: string;
    };
  }
}

export function apiUrl(): string {
  return (window.__env && window.__env.apiUrl) || 'http://localhost:8080';
}

export function keycloakUrl(): string {
  return (window.__env && window.__env.keycloakUrl) || 'http://localhost:8081';
}

export function keycloakRealm(): string {
  return (window.__env && window.__env.keycloakRealm) || 'bibliotheque';
}

export function keycloakClient(): string {
  return 'bibliotheque-frontend';
}
