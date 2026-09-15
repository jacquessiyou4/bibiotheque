# Sécurité

## Signaler une vulnérabilité

Merci de **ne pas ouvrir d'issue publique** pour une faille de sécurité.

Utilisez le signalement privé de GitHub : onglet **Security** du dépôt ›
**Report a vulnerability**. Le message reste visible uniquement des
mainteneurs.

Indiquez si possible :

- la version ou le commit concerné ;
- les étapes pour reproduire (requêtes, comptes de démonstration utilisés) ;
- l'impact estimé (lecture de données d'autrui, élévation de rôle, déni de service…).

Vous recevez un accusé de réception sous 7 jours. Une fois le correctif publié,
le signalement peut être rendu public ; votre contribution est citée si vous le
souhaitez.

## Versions prises en charge

Seule la branche `main` reçoit des correctifs de sécurité.

## Ce qui est déjà en place

| Domaine | Mesure |
|---|---|
| Authentification | Jetons Keycloak (OIDC) validés par signature JWKS et issuer ; jetons d'accès de 15 min |
| Mots de passe | Verrouillage temporaire du compte après 5 échecs (Keycloak) ; 10 tentatives de connexion par minute et par IP côté API (réponse 429) |
| Autorisations | `@PreAuthorize` par rôle ; un adhérent n'agit que sur ses propres emprunts et réservations |
| Comptes | Compte local relié au compte Keycloak par son identifiant immuable (`sub`) |
| Navigateur | Jeton limité à l'onglet (`sessionStorage`), expiration automatique ; CSP stricte servie par nginx |
| Erreurs | Corps `application/problem+json` sans détail technique ; profil `prod` sans message interne ni Swagger |
| Conteneurs | Utilisateur non root pour le backend, `no-new-privileges`, capacités Linux retirées, limites mémoire et CPU |
| Dépendances | Dependabot (Maven, npm, Docker, GitHub Actions) ; `npm audit` et scan Trivy des images en CI |
| Traçabilité | `X-Request-ID` dans chaque log et chaque réponse d'erreur ; journal des refus 401/403 |

Les limites connues (frameworks en fin de support, flux de connexion par mot de
passe plutôt qu'Authorization Code + PKCE, secrets en variables
d'environnement) sont suivies dans [BEST_PRACTICES.md](BEST_PRACTICES.md).
