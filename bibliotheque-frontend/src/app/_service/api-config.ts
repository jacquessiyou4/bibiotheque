// Lit l'URL de l'API depuis window.__env (injecté par assets/env.js).
// Ce fichier est régénéré au démarrage du conteneur frontend à partir de la
// variable d'environnement API_URL, donc rien n'est plus écrit en dur ici.
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

// URL de l'autorité Keycloak : le NAVIGATEUR s'y authentifie (login) puis
// présente le jeton reçu au backend. Configurée via KEYCLOAK_URL dans le
// docker-compose (voir docker-entrypoint.d/40-env-config.sh).
export function keycloakUrl(): string {
  return (window.__env && window.__env.keycloakUrl) || 'http://localhost:8081';
}

export function keycloakRealm(): string {
  return (window.__env && window.__env.keycloakRealm) || 'bibliotheque';
}

export function keycloakClient(): string {
  return 'bibliotheque-frontend';
}
