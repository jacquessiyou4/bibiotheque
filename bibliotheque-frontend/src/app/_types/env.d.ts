export interface EnvConfig {
  apiUrl?: string;
}

declare global {
  interface Window {
    __env?: EnvConfig;
  }
}
