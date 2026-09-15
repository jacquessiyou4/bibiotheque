<h1 align="center">
    <br>
    Bibliothèque
    <br>
</h1>

[![Spring Boot](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)]()
[![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white)]()
[![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)]()
[![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white)]()
[![Maven](https://img.shields.io/badge/apache_maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)]()
[![Bootstrap](https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white)]()

Application full-stack de gestion de bibliothèque : **Spring Boot** (API REST) +
**Angular** (interface) + **MySQL** (persistance).

* Deux profils : **Admin** (CRUD livres et utilisateurs) et **User** (emprunter / rendre).
* Authentification par **JWT**.
* Mots de passe chiffrés avec **BCrypt**.
* Redirection vers une page *forbidden* si le rôle n'a pas accès à l'URL.

---

> **Exploitation** (démarrage, sauvegardes, restauration, pannes courantes) : voir
> [docs/RUNBOOK.md](docs/RUNBOOK.md). Le frontend est servi par nginx sur le port
> 4200 ; le backend (port 8080) n'expose plus que l'API et Swagger.

## Sommaire

1. [Prérequis](#1-prérequis)
2. [État du dépôt : ce qui marche, ce qui ne marche pas](#2-état-du-dépôt--ce-qui-marche-ce-qui-ne-marche-pas)
3. [Arborescence](#3-arborescence)
4. [Démarrer le projet](#4-démarrer-le-projet)
5. [Créer le premier compte](#5-créer-le-premier-compte)
6. [Le trajet d'une donnée : du clic à la base](#6-le-trajet-dune-donnée--du-clic-à-la-base)
7. [Les API](#7-les-api)
8. [Rappel Git](#8-rappel-git)
9. [Captures d'écran](#9-captures-décran)

---

## 1. Prérequis

À installer **avant** la séance. Ne venez pas avec une machine vierge.

| Outil | Version | Vérifier avec |
|---|---|---|
| JDK | 17 ou + | `java -version` |
| Node.js | 20 ou + | `node -v` |
| npm | fourni avec Node | `npm -v` |
| Docker Desktop | à jour, **démarré** | `docker -v` puis `docker compose version` |
| Git | quelconque | `git --version` |
| IDE | IntelliJ IDEA / VS Code | — |

Si l'une de ces commandes ne répond pas, l'outil n'est pas dans votre `PATH` :
c'est à régler avant 8h30, pas pendant l'exercice.

---

## 2. État du dépôt : ce qui marche, ce qui ne marche pas

Ce dépôt est un projet **réel et daté**. Il ne se lance pas tout seul sur une
machine d'aujourd'hui. C'est volontaire : savoir démarrer un projet inconnu,
c'est d'abord savoir diagnostiquer pourquoi il refuse de démarrer.

### Ce qui est déjà là

* Un backend Spring Boot complet : entités, repositories, contrôleurs, sécurité JWT.
* Un frontend Angular complet : 15 composants, routage, guard, intercepteur HTTP.
* Aucune donnée : la base est vide au premier démarrage.

### Ce qui manque ou coince — c'est votre travail

| Constat | Détail |
|---|---|
| **Le backend ne compile pas sur un JDK 17+** | `pom.xml` cible Spring Boot 2.4.5 et Java 1.8. La version de Lombok qu'il embarque ne connaît pas le compilateur des JDK récents. Sur JDK 21, le build s'arrête sur `java.lang.NoSuchFieldError: Class com.sun.tools.javac.tree.JCTree$JCImport does not have member field 'com.sun.tools.javac.tree.JCTree qualid'`. |
| **Le frontend est en Angular 14** | `npx ng version` affiche `Node: 22.x (Unsupported)`. Le build passe malgré tout, mais vous êtes hors du support officiel. |
| **Aucun fichier Docker** | Pas de `Dockerfile`, pas de `docker-compose.yml`. La consigne « lancer avec `docker compose up` » suppose que vous les écriviez. |
| **La base doit exister à la main** | `application.properties` pointe sur `jdbc:mysql://localhost:3306/bibliotheque` avec `root` / `mysql`. Le schéma `bibliotheque` n'est créé par personne. |
| **Aucun compte de départ** | `POST /admin/users` est protégé : impossible de créer le premier administrateur via l'API. Voir la [section 5](#5-créer-le-premier-compte). |
| **L'URL de l'API est en dur** | `http://localhost:8080` est écrit dans les trois services Angular, pas dans `environment.ts`. |

> Ne « corrigez » rien avant qu'on en parle en séance : ces points sont les
> exercices, pas des bugs à masquer.

---

## 3. Arborescence

```
bibliothèque/
├── bibliotheque-backend/            API REST Spring Boot — port 8080, préfixe /api/v1
│   ├── pom.xml
│   └── src/main/java/com/ibizabroker/bibliotheque/
│       ├── BibliothequeApplication.java
│       ├── catalogue/               livres
│       │   ├── api/                 CatalogueApi, LivreResume : ce qu'utilisent les autres fonctionnalités
│       │   ├── web/                 BooksController, BookRequest, BookResponse
│       │   └── internal/            BooksService, BooksRepository, Books (entité)
│       ├── emprunts/                prêts et retours (api/ web/ internal/)
│       ├── reservations/            réservations des livres indisponibles (api/ web/ internal/)
│       ├── utilisateurs/            comptes, profil, lien avec Keycloak (api/ web/ internal/)
│       ├── auth/                    POST /api/v1/auth/token (web/ internal/)
│       ├── donnees/                 export et anonymisation RGPD (web/ internal/)
│       └── shared/                  commun à toutes les fonctionnalités
│           ├── config/              sécurité, CORS, OpenAPI, versionnement de l'API
│           ├── web/                 erreurs problem+json, filtres (requestId, anciens chemins, limite de connexion)
│           ├── error/               exceptions métier avec leur code
│           └── util/                formes canoniques des saisies
│   └── src/main/resources/
│       ├── application.properties, application-prod.properties
│       └── db/migration/            V1 … V5 (Flyway)
│
├── bibliotheque-frontend/           interface Angular — port 4200 (nginx)
│   └── src/app/
│       ├── app.module.ts            coquille : en-tête, accueil, accès refusé
│       ├── app-routing.module.ts    fonctionnalités chargées à la demande
│       ├── core/
│       │   ├── api/                 appels HTTP (livres, emprunts, réservations, utilisateurs)
│       │   ├── auth/                garde de route, intercepteur (jeton, erreurs)
│       │   ├── services/            session, notifications, traduction, thème, erreurs
│       │   ├── i18n/                traductions fr / en
│       │   ├── layout/              header, home, forbidden
│       │   └── routing/             matcher des modules chargés à la demande
│       ├── shared/                  SharedModule, pipe translate, modèles
│       └── features/                catalogue, emprunts, reservations, utilisateurs, auth
│
├── keycloak/realm-bibliotheque.json realm importé au premier démarrage
├── ops/                             sauvegardes (backup/), Prometheus et Grafana (observability/)
├── docs/                            RUNBOOK, ADR, contrat OpenAPI, données personnelles
├── docker-compose.yml               pile complète ; profils https, ops, observability
└── docker-compose.secrets.yml       variante avec mots de passe en fichiers
```

**La règle à retenir** : côté backend, un package = une fonctionnalité métier,
et une fonctionnalité n'utilise les autres que par leur package `api`
(vérifié par `ArchitectureTest`). Côté frontend, une fonctionnalité n'importe
jamais une autre ; ce qu'elles partagent vit dans `core` ou `shared` (vérifié par
`npm run arch`).

---

## 4. Démarrer le projet

### 4.1 La base de données

Le backend ne crée pas le schéma, seulement les tables. Il faut donc :

```sql
CREATE DATABASE bibliotheque;
```

Les identifiants attendus sont dans
[`application.properties`](bibliotheque-backend/src/main/resources/application.properties) :
utilisateur `root`, mot de passe `mysql`, port `3306`. Adaptez le fichier à
votre installation **ou** votre installation au fichier — mais sachez lequel
des deux vous avez fait.

### 4.2 Le backend

```bash
cd bibliotheque-backend
./mvnw spring-boot:run          # Windows : mvnw.cmd spring-boot:run
```

Au démarrage, `spring.jpa.hibernate.ddl-auto=update` demande à Hibernate de
créer les tables manquantes. Vérifiez-le tout de suite :

```sql
USE bibliotheque;
SHOW TABLES;
DESCRIBE books;
```

> Si Maven s'arrête sur `NoSuchFieldError ... JCTree$JCImport`, vous compilez
> avec un JDK trop récent pour ce projet.
> Voir la [section 2](#2-état-du-dépôt--ce-qui-marche-ce-qui-ne-marche-pas).

L'API écoute sur **http://localhost:8080**.

### 4.3 Le frontend

```bash
cd bibliotheque-frontend
npm install
npm start                       # équivaut à : ng serve
```

L'interface est sur **http://localhost:4200**. Elle appelle le backend sur le
port 8080 : les deux doivent tourner en même temps.

---

## 5. Créer le premier compte

> **Mise à jour (Docker + Keycloak + Flyway).** Avec `docker compose up`, cette
> étape est automatique : l'authentification passe par Keycloak
> (`keycloak/realm-bibliotheque.json`) et la migration
> `V2__seed_comptes_et_catalogue.sql` crée les utilisateurs locaux
> correspondants et un petit catalogue. Comptes de test :
> **admin / admin123** (Admin, BIBLIOTHECAIRE), **A1 / A1123** et
> **A2 / A2123** (User, ADHERENT). La procédure SQL ci-dessous ne concerne que
> l'ancienne version MySQL du projet.

Il n'y a aucun utilisateur en base, et `POST /admin/users` exige déjà un token.
Le premier administrateur s'insère donc directement en SQL, **après** le premier
démarrage du backend — sinon les tables n'existent pas encore.

Le mot de passe doit être un hachage **BCrypt** : `WebSecurityConfiguration`
déclare un `BCryptPasswordEncoder`, il n'acceptera jamais un mot de passe en
clair. Le hachage ci-dessous correspond à `admin123`.

```sql
USE bibliotheque;

-- 1. Regardez d'abord ce qu'Hibernate a réellement créé.
--    Les noms ci-dessous suivent la convention Spring Boot
--    (camelCase -> snake_case), mais vérifiez-les, ne les supposez pas.
SHOW TABLES;
DESCRIBE users;
DESCRIBE role;

-- 2. Puis insérez, en adaptant aux colonnes que DESCRIBE vous a montrées.
INSERT INTO role (role_name) VALUES ('Admin'), ('User');

INSERT INTO users (user_id, username, name, password)
VALUES (1, 'admin', 'Administrateur',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696');

INSERT INTO user_role (user_id, role_id)
VALUES (1, (SELECT role_id FROM role WHERE role_name = 'Admin'));

-- 3. Si une table hibernate_sequence existe, avancez son compteur au-delà
--    des identifiants que vous venez de poser à la main, sinon la prochaine
--    création depuis l'application entrera en collision.
UPDATE hibernate_sequence SET next_val = 100 WHERE next_val < 100;
```

Connexion : **admin / admin123**.

Vérification en ligne de commande, sans passer par le navigateur :

```bash
curl -X POST http://localhost:8080/auth/token \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
```

Vous devez recevoir un JSON contenant `accessToken`. Gardez-le : il sert pour
tous les autres appels.

```bash
curl http://localhost:8080/admin/users -H "Authorization: Bearer <le_token>"
```

---

## 6. Le trajet d'une donnée : du clic à la base

C'est l'objectif de la séance. Prenons **la création d'un livre** et suivons-la
couche par couche. Ouvrez les fichiers au fur et à mesure : ne lisez pas ce
tableau passivement.

| # | Où | Fichier | Ce qui se passe |
|---|---|---|---|
| 1 | Navigateur | [`create-book.component.html`](bibliotheque-frontend/src/app/create-book/create-book.component.html) | Vous remplissez le formulaire. `[(ngModel)]` recopie chaque champ dans l'objet `book` au fil de la frappe. |
| 2 | Navigateur | [`create-book.component.ts`](bibliotheque-frontend/src/app/create-book/create-book.component.ts) | Le clic déclenche `onSubmit()` → `saveBook()` → `booksService.createBook(this.book)`. |
| 3 | Navigateur | [`books.service.ts`](bibliotheque-frontend/src/app/_service/books.service.ts) | Traduit l'appel en `POST http://localhost:8080/admin/books`, objet sérialisé en JSON. |
| 4 | Navigateur | [`auth.interceptor.ts`](bibliotheque-frontend/src/app/_auth/auth.interceptor.ts) | **Toute** requête sortante passe ici : il ajoute l'en-tête `Authorization: Bearer <token>`. C'est lui aussi qui redirige vers `/login` sur un 401 et vers `/forbidden` sur un 403. |
| 5 | Réseau | — | La requête quitte le navigateur. Ouvrez l'onglet *Réseau* des DevTools : vous devez voir le POST, son corps et son en-tête. |
| 6 | Backend | [`CorsConfiguration.java`](bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/CorsConfiguration.java) | Le port 4200 n'est pas le port 8080 : sans cette autorisation CORS, le navigateur refuserait la réponse. |
| 7 | Backend | [`KeycloakJwtConfiguration.java`](bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/KeycloakJwtConfiguration.java) | Le resource server OAuth2 vérifie la signature et l'issuer du jeton Keycloak, puis traduit ses rôles en autorités Spring posées dans le `SecurityContext`. Exécuté **avant** tout contrôleur. |
| 8 | Backend | [`WebSecurityConfiguration.java`](bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java) | Décide si la requête a le droit de continuer. Sans authentification valide → 401 émis par `JwtAuthenticationEntryPoint`. |
| 9 | Backend | [`BooksController.java`](bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/BooksController.java) | `@PostMapping("/books")` reçoit le JSON, `@RequestBody` le transforme en objet `Books`. `@PreAuthorize("hasRole('Admin')")` refuse si le rôle ne colle pas → 403. |
| 10 | Backend | [`BooksRepository.java`](bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/BooksRepository.java) | `save(book)`. L'interface est vide : Spring Data en génère l'implémentation au démarrage. |
| 11 | Backend | [`Books.java`](bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Books.java) | `@Entity` / `@Table(name = "Books")` : c'est cette classe qui dit à Hibernate quelle table et quelles colonnes viser. |
| 12 | Base | MySQL | Hibernate émet l'`INSERT`. `spring.jpa.show-sql=true` l'affiche dans la console : lisez-le, c'est la preuve que le trajet est complet. |
| 13 | Retour | — | L'objet sauvegardé (avec son `bookId`) repart en JSON, le `subscribe()` de l'étape 2 se déclenche et route vers `/books`. |

Le même trajet vaut pour la lecture, la modification et la suppression : seuls
le verbe HTTP et la méthode du repository changent.

**Exercice de lecture** : refaites ce tableau, seul, pour l'emprunt d'un livre
(`borrow-book` → `BorrowController`). Vous y trouverez une différence notable :
le contrôleur y modifie **deux** tables.

---

## 7. Les API

Base : `http://localhost:8080/api/v1` — contrat complet dans Swagger
(`http://localhost:8080/swagger-ui.html`) et dans [docs/api/openapi.json](docs/api/openapi.json).

> **Anciens chemins** (`/admin/books`, `/borrow`, `/api/reservations`, `/auth/token`,
> `/profile`…) : toujours servis, mais obsolètes. Leurs réponses portent les
> en-têtes `Deprecation: true`, `Sunset` (suppression prévue le 15 mars 2027) et
> `Link` vers le chemin `/api/v1` équivalent.

**Erreurs** : toutes au format `application/problem+json` :

```json
{ "type": "https://bibliotheque.local/problemes/book-unavailable", "title": "Bad Request",
  "status": 400, "code": "BOOK_UNAVAILABLE", "detail": "Le livre \"L2\" n'est plus disponible.",
  "requestId": "3f2a9c1e-…" }
```

Le `code` est stable (à utiliser dans un client) ; le `requestId` permet de
retrouver la requête dans les logs (voir [docs/RUNBOOK.md](docs/RUNBOOK.md)).

### Authentification

`POST /api/v1/auth/token` — accessible sans token, renvoie les jetons Keycloak
(`accessToken`, `refreshToken`, durées de validité). Au-delà de 10 tentatives par
minute depuis une même adresse : `429`.

```json
{ "username": "admin", "password": "admin123" }
```

`GET /api/v1/profile` — profil de l'utilisateur du jeton (id, username, nom, email, rôles).

`GET /api/v1/profile/export` — toutes les données personnelles de l'utilisateur
(profil, emprunts, réservations), en fichier JSON.

### Livres — `/api/v1/books`

| Verbe | URL | Rôle | Description |
|---|---|---|---|
| GET | `/api/v1/books` | authentifié | Liste paginée des livres |
| GET | `/api/v1/books/{id}` | Admin | Un livre par son id |
| POST | `/api/v1/books` | Admin | Crée un livre |
| PUT | `/api/v1/books/{id}` | Admin | Modifie un livre |
| DELETE | `/api/v1/books/{id}` | Admin | Supprime un livre (refusé en `409` s'il est encore emprunté) |

```json
{
    "bookName": "Le Petit Prince",
    "bookAuthor": "Antoine de Saint-Exupéry",
    "bookGenre": "Conte",
    "noOfCopies": 5
}
```

### Utilisateurs — `/api/v1/users`

| Verbe | URL | Rôle | Description |
|---|---|---|---|
| GET | `/api/v1/users` | Admin | Liste paginée des utilisateurs |
| GET | `/api/v1/users/{id}` | Admin | Un utilisateur par son id |
| POST | `/api/v1/users` | Admin | Crée un utilisateur (username enregistré en minuscules) |
| PUT | `/api/v1/users/{id}` | Admin | Modifie un utilisateur |
| POST | `/api/v1/users/{id}/anonymisation` | Admin | Anonymise le compte (droit à l'effacement, irréversible) |

```json
{
    "username": "marie",
    "name": "Marie Dupont",
    "password": "motdepasse",
    "roles": [ "User", "ADHERENT" ]
}
```

### Emprunts — `/api/v1/loans`

| Verbe | URL | Description |
|---|---|---|
| GET | `/api/v1/loans` | Tous les emprunts (bibliothécaire) |
| GET | `/api/v1/loans/user/{id}` | Les emprunts d'un utilisateur (un adhérent : les siens) |
| GET | `/api/v1/loans/book/{id}` | L'historique d'un livre (bibliothécaire) |
| POST | `/api/v1/loans` | Emprunter : retire un exemplaire, échéance à 7 jours (`201`) |
| PUT | `/api/v1/loans` | Rendre : remet l'exemplaire en stock, pose la date de retour |

```json
{ "bookId": 3, "userId": 5 }
```

Les dates sont au format ISO-8601 (`2026-09-15T10:15:30`).

### Réservations — `/api/v1/reservations`

| Verbe | URL | Description |
|---|---|---|
| GET | `/api/v1/reservations` | Liste (un adhérent : les siennes) |
| GET | `/api/v1/reservations/{id}` | Une réservation |
| POST | `/api/v1/reservations` | Réserver un livre indisponible |
| PATCH | `/api/v1/reservations/{id}/annuler` | Annuler |
| DELETE | `/api/v1/reservations/{id}` | Supprimer (bibliothécaire) |
| GET | `/api/v1/reservations/expirees` | Réservations expirées (bibliothécaire) |

---

## 8. Rappel Git

Le cycle complet, dans l'ordre, à savoir refaire sans regarder :

```bash
# 1. Partir d'une base à jour
git checkout main
git pull

# 2. Une branche par sujet. Nommez-la pour qu'on devine son contenu.
git switch -c feat/nom-du-sujet

# 3. Travailler, puis regarder ce qu'on s'apprête à livrer
git status
git diff

# 4. Choisir ce qui entre dans le commit — pas de "git add ." aveugle
git add <fichiers>
git commit -m "feat: message à l'impératif, une ligne, ce qui change et pourquoi"

# 5. Publier la branche
git push -u origin feat/nom-du-sujet

# 6. Ouvrir la Pull Request sur GitHub, et y décrire :
#    ce que ça fait, comment le tester, ce qui reste à faire.
```

Quelques réflexes :

* `git log --oneline --graph --all` pour voir où vous en êtes.
* Un commit = un changement cohérent. Dix fichiers sans rapport dans un commit,
  c'est une revue impossible.
* On ne pousse jamais sur `main` directement.
* `node_modules/`, `target/` et `dist/` ne sont **jamais** commités : c'est le
  rôle des `.gitignore` du dépôt. Si `git status` vous les propose, quelque
  chose ne va pas.

---

## 9. Captures d'écran

### Accueil et connexion

![Page d'accueil](./screenshots/home.png "Page d'accueil")
![Page de connexion](./screenshots/login.png "Page de connexion")

### Côté administrateur

| Écran | Aperçu |
|---|---|
| Liste des livres | ![Liste des livres](./screenshots/book_list.png) |
| Ajout d'un livre | ![Ajout d'un livre](./screenshots/book_add.png) |
| Modification | ![Modification](./screenshots/book_update.png) |
| Historique d'un livre | ![Détail livre](./screenshots/book_details.png) |
| Liste des utilisateurs | ![Liste des utilisateurs](./screenshots/user_list.png) |
| Ajout d'un utilisateur | ![Ajout utilisateur](./screenshots/user_add.png) |
| Emprunts d'un utilisateur | ![Détail utilisateur](./screenshots/user_details.png) |

### Côté utilisateur

| Écran | Aperçu |
|---|---|
| Emprunter | ![Emprunter](./screenshots/borrow_book.png) |
| Rendre | ![Rendre](./screenshots/return_book.png) |
| Accès refusé | ![Forbidden](./screenshots/forbidden.png) |
