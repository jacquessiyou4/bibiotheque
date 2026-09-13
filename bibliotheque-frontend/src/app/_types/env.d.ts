export interface EnvConfig {
  apiUrl?: string;
  keycloakUrl?: string;
  keycloakRealm?: string;
}

declare global {
  interface Window {
    __env?: EnvConfig;
  }
}
