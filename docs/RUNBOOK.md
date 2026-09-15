# Runbook — Bibliothèque

Procédures d'exploitation de la pile Docker : démarrer, vérifier, sauvegarder,
restaurer, réagir aux pannes courantes. Chaque commande se lance depuis la
racine du dépôt.

## 1. Démarrer et arrêter

| Action | Commande |
|---|---|
| Démarrer toute la pile | `docker compose up -d` |
| Avec HTTPS (Caddy) | `docker compose --profile https up -d` |
| Avec les sauvegardes quotidiennes | `docker compose --profile ops up -d backup` |
| Avec la supervision (Prometheus + Grafana, § 9) | `docker compose --profile observability up -d prometheus grafana` |
| Reconstruire après un changement de code | `docker compose up -d --build backend frontend` |
| Arrêter (sans perte de données) | `docker compose stop` |
| Supprimer les conteneurs (volumes conservés) | `docker compose down` |

> **Ne jamais lancer `docker compose down -v` sur un environnement réel** :
> `-v` supprime les volumes `db_data` et `keycloak_db_data`, donc tous les
> livres, emprunts, réservations et comptes Keycloak.

L'arrêt est progressif : le backend termine les requêtes en cours (20 s au
plus, `server.shutdown=graceful`) avant de s'arrêter.

## 2. Vérifier que tout va bien

| Service | Vérification | Attendu |
|---|---|---|
| Tous | `docker compose ps` | `healthy` partout |
| Backend prêt | `curl -s localhost:8080/actuator/health/readiness` | `{"status":"UP"}` |
| Backend vivant | `curl -s localhost:8080/actuator/health/liveness` | `{"status":"UP"}` |
| Keycloak | `curl -s -o /dev/null -w '%{http_code}' localhost:8081/realms/bibliotheque` | `200` |
| Frontend | navigateur sur `http://localhost:4200` | page de connexion |

Au démarrage, le backend n'est `healthy` qu'une fois les migrations Flyway
appliquées ; le frontend attend cet état avant de démarrer.

## 3. Retrouver une erreur signalée par un utilisateur

Chaque réponse d'erreur de l'API contient un `requestId`, que l'interface
affiche en fin de message : « … (réf. 3f2a9c1e) ». Toutes les lignes de log de
la requête portent le même identifiant :

```bash
docker compose logs backend | grep 3f2a9c1e
```

Le champ `code` de la réponse (ex. `BOOK_UNAVAILABLE`, `CONCURRENT_UPDATE`)
indique la règle en cause sans avoir à lire le message.

## 4. Sauvegardes

Le service `backup` (profil `ops`) sauvegarde **les deux bases** (application et
Keycloak) chaque jour à `BACKUP_HEURE` UTC dans `BACKUP_DIR` (par défaut
`./backups`, hors Git) et garde `BACKUP_JOURS_CONSERVES` jours.

```bash
# Sauvegarde immédiate (avant une mise à jour, une migration...)
docker compose --profile ops run --rm backup once

# Sauvegardes présentes
ls -lh backups/
```

Chaque fichier est relu par `pg_restore --list` juste après sa création : un
dump illisible fait échouer la sauvegarde immédiatement.

Copier régulièrement `backups/` **hors de la machine** (autre disque, stockage
objet) : une sauvegarde sur le même disque ne protège pas d'une panne de disque.

## 5. Restaurer

Toujours restaurer d'abord dans une base de vérification, jamais directement
par-dessus la production.

```bash
# 1. Base de vérification temporaire sur le réseau de la pile
docker run -d --name restore-verif --network bibiotheque_default \
  -e POSTGRES_PASSWORD=verif -e POSTGRES_DB=verif postgres:16-alpine

# 2. Restauration du dump choisi
docker cp backups/bibliotheque-AAAAMMJJTHHMMSSZ.dump restore-verif:/tmp/app.dump
docker cp ops/backup/restauration.sh restore-verif:/tmp/
docker exec -e PGPASSWORD=verif restore-verif sh /tmp/restauration.sh /tmp/app.dump 127.0.0.1 verif postgres

# 3. Contrôle du contenu
docker exec restore-verif psql -U postgres -d verif -c "select count(*) from books; select max(version) from flyway_schema_history;"

# 4. Nettoyage
docker rm -f restore-verif
```

Pour remplacer réellement la base de production, après vérification :
arrêter `backend` (`docker compose stop backend`), recréer la base vide, y
restaurer le dump avec la même commande, puis `docker compose start backend`.

**Exercice de restauration** : à faire après chaque changement de schéma et au
moins une fois par trimestre. Dernier exercice réussi : 15 septembre 2026
(dump de l'application restauré, livres, utilisateurs et migrations présents).

## 6. Keycloak

### Compte verrouillé

Après 5 mots de passe erronés, Keycloak bloque le compte temporairement (1 min,
puis jusqu'à 15 min si les échecs continuent). Pour le débloquer tout de suite :
console `http://localhost:8081` › realm *bibliotheque* › *Users* › le compte ›
*Temporarily locked* › désactiver.

L'API refuse aussi plus de 10 tentatives de connexion par minute depuis une
même adresse IP (réponse `429`, code `TOO_MANY_LOGIN_ATTEMPTS`) ; il suffit
d'attendre le délai indiqué dans l'en-tête `Retry-After`.

### Exporter le realm après une modification dans la console

`keycloak/realm-bibliotheque.json` n'est lu qu'à la **création** du realm. Une
modification faite dans la console n'y est pas reportée : l'exporter pour la
versionner.

```bash
docker compose exec keycloak /opt/keycloak/bin/kc.sh export \
  --realm bibliotheque --file /tmp/realm.json --users skip
docker compose cp keycloak:/tmp/realm.json keycloak/realm-bibliotheque.json
```

Relire le diff avant de valider : l'export contient aussi des valeurs par défaut.

### Avant une mise en production

Le realm fourni sert au développement. Pour un environnement réel :

- supprimer les comptes de démonstration (`admin`, `A1`, `A2`) et leurs mots de
  passe connus ;
- ajouter une politique de mots de passe (*Authentication › Policies*, par
  exemple `length(12) and notUsername`). Elle n'est pas dans le realm de
  démonstration, car les mots de passe `A1123` / `A2123` y sont trop courts et
  Keycloak refuse alors d'importer le realm ;
- démarrer Keycloak en mode production (`start --optimized`) derrière Caddy.

## 7. Changer un secret

| Secret | Où | Procédure |
|---|---|---|
| Mot de passe PostgreSQL de l'application | `.env` : `POSTGRES_PASSWORD` | `ALTER USER bibliotheque PASSWORD '…';` dans `db`, mettre `.env` à jour, `docker compose up -d backend backup` |
| Mot de passe de la base Keycloak | `.env` : `KEYCLOAK_DB_PASSWORD` | même procédure dans `keycloak-db`, puis `docker compose up -d keycloak backup` |
| Admin Keycloak | console › *master* › *Users* › admin | changer le mot de passe dans la console ; `KEYCLOAK_ADMIN_PASSWORD` ne sert qu'au premier démarrage |
| Accès de Prometheus aux métriques | `.env` : `METRICS_PASSWORD` | nouvelle valeur dans `.env`, puis `docker compose up -d backend` et `docker compose --profile observability up -d --force-recreate prometheus` |

Le fichier `.env` n'est jamais versionné (voir `.env.example`).

### Secrets en fichiers (recommandé hors poste de développement)

Avec `docker-compose.secrets.yml`, les mots de passe ne sont plus dans `.env` ni
dans l'environnement des conteneurs (`docker inspect` ne les montre pas) : ils
sont lus dans `secrets/` (non versionné, droits `600`).

```bash
mkdir -p secrets && chmod 700 secrets
for s in postgres_password keycloak_db_password keycloak_admin_password grafana_admin_password; do
  openssl rand -base64 32 | tr -d '\n' > "secrets/$s"
done
chmod 600 secrets/*
docker compose -f docker-compose.yml -f docker-compose.secrets.yml up -d
```

Changer un secret : écrire la nouvelle valeur dans le fichier, l'appliquer à la
base (`ALTER USER … PASSWORD …`), puis recréer les services qui l'utilisent
(`docker compose -f docker-compose.yml -f docker-compose.secrets.yml up -d --force-recreate backend backup`).

> Passer d'un démarrage avec `.env` à un démarrage avec secrets ne change pas le
> mot de passe **déjà enregistré** dans le volume PostgreSQL : la base garde celui
> de sa création. Mettre la base à jour avec `ALTER USER` avant de basculer.

## 8. Pannes courantes

| Symptôme | Cause probable | Action |
|---|---|---|
| Backend `unhealthy`, logs `V4 : usernames en double…` | Données incompatibles avec une contrainte de la migration V4 | La migration a été annulée sans rien modifier ; corriger les données avec la requête indiquée dans le message, puis redémarrer le backend |
| Frontend ne démarre pas | Il attend que le backend soit `healthy` | Voir les logs du backend |
| Keycloak met plusieurs minutes à démarrer | Mode `start-dev` sur une machine chargée | Attendre (healthcheck : jusqu'à 5 min) ; vérifier la mémoire libre (`free -h`) |
| Connexion : « Identifiants incorrects » avec le bon mot de passe | Compte verrouillé (§ 6) | Débloquer le compte |
| `403` avec le code `ACCOUNT_LINK_MISMATCH` | Le username a été attribué à un autre compte Keycloak que celui relié au compte local | Vérifier dans Keycloak ; si c'est voulu, vider `users.keycloak_sub` pour ce compte local |
| `409` avec le code `CONCURRENT_UPDATE` | Deux personnes ont modifié la même donnée | Recharger la page et refaire la modification |
| Conteneur `prometheus` arrêté, log « Définir METRICS_PASSWORD… » | `METRICS_PASSWORD` vide dans `.env` | Voir § 9 ; le reste de la pile n'est pas concerné |

## 9. Supervision (Prometheus et Grafana)

Le profil `observability` ajoute Prometheus (collecte et alertes) et Grafana
(tableaux de bord). Il est facultatif : sans lui, la pile fonctionne à l'identique.

```bash
# Une fois : mot de passe partagé par le backend et Prometheus
echo "METRICS_PASSWORD=$(openssl rand -base64 24)" >> .env
docker compose up -d backend          # le backend relit METRICS_PASSWORD
docker compose --profile observability up -d prometheus grafana
```

| Outil | Adresse | Accès |
|---|---|---|
| Prometheus | `http://127.0.0.1:9090` (`PROMETHEUS_PORT`) | depuis la machine hôte uniquement |
| Grafana | `http://127.0.0.1:3000` (`GRAFANA_PORT`) | `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` ; tableau « Bibliothèque — santé de l'API » |

Prometheus lit `/actuator/prometheus` en HTTP Basic (utilisateur `prometheus`,
mot de passe `METRICS_PASSWORD`), car il ne peut pas obtenir de jeton Keycloak.
Si `METRICS_PASSWORD` est vide, le backend ferme cet accès (seul un
administrateur connecté lit alors les métriques) et le conteneur Prometheus
s'arrête dès son démarrage avec un message explicite.

**Alertes** (`ops/observability/alertes.yml`, page *Alerts* de Prometheus) :
`BackendInjoignable`, `TauxErreurs5xxEleve`, `LatenceP95Elevee`,
`ConnexionsRefuseesEnSerie`, `PoolConnexionsSature`.

**Compteurs métier** : les métriques préfixées `bibliotheque_` comptent les
emprunts, retours, réservations, réservations expirées et connexions ; à
interroger dans Prometheus ou à ajouter au tableau Grafana.
