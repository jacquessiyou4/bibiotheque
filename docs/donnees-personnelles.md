# Données personnelles

Inventaire des données personnelles traitées par l'application, de leur durée
de conservation et des procédures d'accès et d'effacement (RGPD, articles 15
et 17). À tenir à jour à chaque ajout de donnée (voir le modèle de pull request).

## Inventaire

| Donnée | Où | Finalité | Base légale |
|---|---|---|---|
| Nom, identifiant de connexion | `users` (application), Keycloak | Identifier l'adhérent, afficher son nom | Exécution du service |
| Adresse e-mail | Keycloak uniquement | Connexion, réinitialisation du mot de passe | Exécution du service |
| Mot de passe | Keycloak uniquement (haché) | Authentification | Exécution du service |
| Identifiant Keycloak (`sub`) | `users.keycloak_sub` | Relier le compte local au compte Keycloak | Exécution du service |
| Rôles | `user_role`, Keycloak | Autorisations | Exécution du service |
| Historique des emprunts | `borrow` | Gestion des prêts et retours | Exécution du service |
| Réservations | `reservation` | File d'attente des livres indisponibles | Exécution du service |
| Adresse IP, identifiant de requête | Logs du backend | Sécurité (limitation des connexions, audit des refus) | Intérêt légitime |

Aucune donnée n'est transmise à un tiers. Les sauvegardes (`backups/`)
contiennent l'ensemble de ces données et suivent la même rétention.

## Durées de conservation

| Donnée | Durée |
|---|---|
| Compte et historique d'un adhérent actif | Tant que le compte est actif |
| Compte fermé | Anonymisation à la demande ou au plus tard 12 mois après la fermeture |
| Réservations annulées ou expirées | 12 mois |
| Logs applicatifs | 30 jours |
| Sauvegardes | `BACKUP_JOURS_CONSERVES` jours (7 par défaut) |

## Droit d'accès et de portabilité

L'adhérent connecté télécharge ses données (profil, emprunts, réservations) :

```
GET /profile/export        (jeton de l'adhérent)
```

La réponse est un fichier JSON (`mes-donnees-bibliotheque.json`). Les données
détenues par Keycloak (e-mail, historique de connexion) s'exportent depuis la
console Keycloak ou le compte utilisateur Keycloak.

## Droit à l'effacement

Un administrateur anonymise le compte local :

```
POST /admin/users/{id}/anonymisation      (jeton Admin)
```

- nom et identifiant sont remplacés (`anonyme-{id}`) ; le mot de passe local,
  le lien Keycloak et les rôles sont supprimés ;
- les emprunts et réservations restent, rattachés à ce compte anonyme : le
  stock et les statistiques de prêt restent justes, sans identifier personne ;
- l'opération est irréversible et journalisée (`[DONNEES] Anonymisation`).

**Étape manuelle** : supprimer ensuite le compte dans Keycloak (console ›
*Users* › le compte › *Delete*). Sinon la personne pourrait encore s'authentifier,
sans toutefois retrouver de données : le compte local ne porte plus son identifiant.
