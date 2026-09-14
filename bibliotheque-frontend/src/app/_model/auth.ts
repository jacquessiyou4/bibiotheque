/** Réponse de POST /auth/token : jetons émis par Keycloak, obtenus via le backend. */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  scope: string;
}

/** Réponse de GET /profile : utilisateur local associé au jeton. */
export interface Profile {
  userId: number;
  username: string;
  name: string;
  email: string | null;
  roles: string[];
}
