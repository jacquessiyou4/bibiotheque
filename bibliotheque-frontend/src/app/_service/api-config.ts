// Lit l'URL de l'API depuis window.__env (injecté par assets/env.js).
// Ce fichier est régénéré au démarrage du conteneur frontend à partir de la
// variable d'environnement API_URL, donc rien n'est plus écrit en dur ici.
declare global {
  interface Window {
    __env?: { apiUrl?: string };
  }
}

export function apiUrl(): string {
  return (window.__env && window.__env.apiUrl) || 'http://localhost:8080';
}
