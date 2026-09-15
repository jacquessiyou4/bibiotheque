# Bibliothèque after the refactor, section by section

The branch `feature/dimension-roadmap-jacques-siyou-noukimi` has four main parts:
the **backend** (the API), the **frontend** (the web pages), the
**infrastructure** (Docker, backups, monitoring) and the **project files**
(docs, CI, governance).

## 1. Backend (`bibliotheque-backend`)

**Before:** code was sorted by technical role (`controller/`, `service/`,
`dao/`, `entity/`…), so each business feature was spread across many folders.
**Now:** it's sorted by business feature. Each feature is a small module with
three layers:

| Layer | Role | Visibility |
|---|---|---|
| `web/` | HTTP endpoints plus request/response objects (DTOs) | public |
| `api/` | the interface other features are allowed to call | public |
| `internal/` | entities, database access, business logic | private to the feature |

| Module | What it does |
|---|---|
| `catalogue` | Books: list, create, update, delete. Other features see only `CatalogueApi` and `LivreResume` |
| `emprunts` | Loans and returns. Stock goes down in one atomic SQL update, so two people can't borrow the last copy |
| `reservations` | Reservations and the security rules RS-01 to RS-05 |
| `utilisateurs` | Accounts, `/profile`, admin screens. Local accounts are linked to Keycloak by its user id (`sub`), not only by username |
| `auth` | `POST /auth/token`: logs in through Keycloak and returns the access and refresh tokens |
| `donnees` | GDPR: export of your personal data (`/profile/export`) |
| `shared` | Common pieces (next table) |

**Inside `shared/`:**

- **`config`:** security, CORS, Keycloak JWT checks, Swagger, API version, and
  `ReglesBibliotheque`. That last one holds the library rules, such as loan and
  hold durations, as settings instead of hard-coded numbers.
- **`error`, `web/ProblemeHttp`, `GlobalExceptionHandler`:** every error comes
  back in one format, with a `code` (e.g. `BOOK_UNAVAILABLE`) and a `requestId`.
- **`web` filters:**
  - request-id tracing plus a log of every refused access
  - a limit on login attempts (429 after 10 per minute)
  - an `/api/v1` version header
  - password protection for the metrics endpoint
- **`observabilite`:** business counters (loans, returns, reservations,
  expirations, logins).
- **`util/Canonical`:** cleans input text (trims spaces, lowercases usernames).

**What else changed:**

- **Legacy login removed:** `/authenticate`, `jjwt` and `JWT_SECRET` are gone.
  Keycloak is the only login.
- **Database migrations:** V4 adds unique usernames, `CHECK` constraints,
  foreign keys and indexes. V5 adds the link to the Keycloak account.
- **Settings:** `application-prod.properties` hides error details and Swagger in
  production. `logback-spring.xml` configures logging.
- **Architecture tests (ArchUnit):** 13 rules check that no feature reaches into
  another's `internal/` package and that features never depend on each other in
  a circle. The build fails if a rule is broken.
- **Tests:** 331 unit and integration tests, plus 10 E2E tests against a real
  Keycloak and PostgreSQL.

## 2. Frontend (`bibliotheque-frontend/src/app`)

**Before:** one big Angular module with 20 components, all loaded at startup.
**Now:** three layers, and each feature is loaded only when you open it.

| Folder | Contents |
|---|---|
| `core/` | The single copies of app-wide services:<br>• `api` services (books, borrow, reservation, users)<br>• `auth` guard and interceptor<br>• `i18n` translations, theme, notifications, error handling<br>• `layout` (header, home, forbidden page) and `routing/chemins.ts` (route paths) |
| `shared/` | Data models, the translate pipe, shared types |
| `features/` | 5 feature modules: `auth`, `catalogue`, `emprunts`, `reservations`, `utilisateurs` |

- **Architecture rules:** `npm run arch` (dependency-cruiser) blocks imports
  between features and cycles.
- **Quality:** `ng lint` (ESLint), an accessibility spec, and 284 tests.
- **Node version:** build with Node 18, as the Dockerfile and CI do. Angular 14's
  production build fails on Node 22.

## 3. Infrastructure

**`docker-compose.yml` services:**

| Service | Role | Starts |
|---|---|---|
| `db`, `backend`, `frontend`, `keycloak-db`, `keycloak` | the application | always |
| `caddy` | HTTPS reverse proxy | profile `https` |
| `backup` | nightly `pg_dump` of both databases, each dump checked after it's written | profile `ops` |
| `prometheus` + `grafana` | metrics, 5 alerts, a ready-made dashboard | profile `observability` |

- **`docker-compose.secrets.yml`:** reads passwords from files instead of `.env`.
- **`ops/`:** backup and restore scripts, plus the Prometheus and Grafana config.
- **Backend image:** runs as a non-root user and no longer bundles Angular, so
  rebuilds are faster. JVM memory is sized to the container.
- **`keycloak/`:** the realm export, with brute-force protection enabled.

## 4. Documentation and governance

- **`docs/RUNBOOK.md`:** how to operate the app: start, check, back up, restore,
  Keycloak, changing secrets, common failures, monitoring.
- **`docs/adr/`:** four architecture decisions:
  - Keycloak as identity provider
  - one shared database schema
  - a monolith split by feature, without Maven modules
  - a glossary and naming rules
- **`docs/api/openapi.json`:** the API contract. CI compares it against `main`
  to catch breaking changes.
- **`docs/donnees-personnelles.md`:** the GDPR data inventory.
- **`SECURITY.md`, `.github/CODEOWNERS`, the pull request template,
  `dependabot.yml`.**

## 5. CI/CD (`.github/workflows`)

**`ci.yml`:**

1. Detects which parts changed, so a frontend-only change skips the backend jobs.
2. **Backend:** tests, coverage check, API contract comparison.
3. **E2E:** real Keycloak and PostgreSQL.
4. **Frontend:** dependency audit, architecture rules, lint, tests with coverage,
   build.
5. **Images:** builds both Docker images and scans them with Trivy.

**`release.yml` with release-please:** creates version tags and a CHANGELOG from
the commit messages.

## What's left

- **Item 15:** the Java 17, Spring Boot 3 and Angular 17+ upgrade.
- **Clutter in the repo root:** the course files (PDF, `SEANCE-1.md`…), the stray
  "Ollama" file and the saved Performance Plan HTML are still there.
- **Legacy guide:** `BEST_PRACTICES.md` still mixes finished and open items.
