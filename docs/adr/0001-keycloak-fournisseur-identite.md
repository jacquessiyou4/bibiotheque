# 0001 — Keycloak comme fournisseur d'identité

- **Statut** : Acceptée
- **Date** : 15 septembre 2026

## Contexte

L'application gérait elle-même les comptes : login `POST /authenticate`, jetons
signés par une clé partagée (`JWT_SECRET`, bibliothèque jjwt), mots de passe
BCrypt en base. Deux systèmes d'authentification coexistaient après
l'introduction de Keycloak, et le jeton maison n'était plus accepté par l'API.

## Décision

- Keycloak est la **seule** source d'authentification : comptes, mots de passe,
  verrouillage après échecs, durée des jetons (realm `bibliotheque`).
- Le backend est un *resource server* OAuth2 : il valide la signature (JWKS) et
  l'issuer des jetons, et lit les rôles dans `realm_access.roles`.
- `POST /auth/token` échange identifiant et mot de passe contre des jetons
  Keycloak pour le frontend et Swagger ; il n'y a pas d'endpoint de
  rafraîchissement.
- Le compte local (`users`) garde les données métier (emprunts, réservations) et
  est relié au compte Keycloak par le claim `sub`, immuable.
- Le login historique (`/authenticate`, `/me`, jjwt, `JWT_SECRET`) est supprimé.

## Conséquences

- Plus de secret de signature à gérer dans l'application.
- Les politiques de sécurité (mots de passe, verrouillage) se règlent dans
  Keycloak, sans redéploiement du backend.
- Keycloak devient une dépendance de démarrage et de test (E2E avec un vrai
  Keycloak via Testcontainers).
- Le flux par mot de passe (*Resource Owner Password Credentials*) disparaît
  d'OAuth 2.1. La cible à terme est Authorization Code + PKCE depuis le
  navigateur.
- Sans rafraîchissement, une session dure au plus la vie du jeton (15 min) :
  l'utilisateur se reconnecte ensuite.
