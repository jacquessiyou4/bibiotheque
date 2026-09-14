# Best Practices — Bibliothèque

> Recommandations concrètes pour améliorer la sécurité, l'architecture et la
> qualité du code de l'application de gestion de bibliothèque.
>
> Classées par priorité : 🔴 critique → 🟠 haute → 🟡 moyenne → 🟢 nice-to-have.

---

## 🔴 Priorité critique — Sécurité & fiabilité

### ~~1. Supprimer tous les `console.log` du code frontend~~

26 instances de `console.log` / `console.warn` / `console.error` sont
présentes dans les composants Angular. En production, elles exposent des
données internes (tokens, objets métier, erreurs) dans la console du
navigateur.

**Avant :**

```typescript
// borrow-book.component.ts
this.borrowService.borrowBook(this.borrow).subscribe(data => {
  console.log(data);          // ← fuite d'information
  ...
}, error => console.log(error)); // ← fuite d'erreur
```

**Après :**

```typescript
// Utiliser un service de logging centralisé ou supprimer purement
this.borrowService.borrowBook(this.borrow).subscribe({
  next: (data) => this.router.navigate(['/borrow-list']),
  error: (err) => this.notificationService.showError('Emprunt impossible')
});
```

> **Outils recommandés :**
> - [`ngx-logger`](https://www.npmjs.com/package/ngx-logger) pour le
>   frontend (niveaux de log configurables, désactivables en prod).
> - SLF4J / Lombok `@Slf4j` pour le backend (déjà partiellement utilisé
>   dans les composants de sécurité).

---

### 2. Mettre à jour les frameworks (EOL)

| Technologie | Version actuelle | Dernière version stable | Risque |
|---|---|---|---|
| Spring Boot | 2.4.5 (mai 2021) | 3.3+ | CVE connues, plus de patchs |
| Angular | 14 (juin 2022) | 18+ | Fin de support LTS |
| Java (target) | 1.8 | 21 LTS | Lombok incompatible JDK 17+ |
| Node.js (build) | 18 | 22 LTS | Angular 14 hors support officiel |

**Actions :**

- **Spring Boot 3.x** : migration vers Jakarta EE (`javax.*` → `jakarta.*`),
  Java 17 minimum, suppression de `WebSecurityConfigurerAdapter` (deprecated).
- **Angular 17+** : signaux (signals), standalone components, esbuild.
- **Java 17+** : corriger le `pom.xml` pour la version de Lombok compatible
  avec l'API interne du compilateur JDK 17+.

---

### 3. Ajouter la terminaison HTTPS

Keycloak a `sslRequired: "external"` mais ni le backend ni le frontend ne
terminent le TLS. Trafiquer les jetons JWT en clair sur le réseau local.

**Solution recommandée :** Ajouter un reverse proxy (Traefik, Caddy ou nginx)
devant tous les services :

```yaml
# Ajout dans docker-compose.yml
  traefik:
    image: traefik:v3.0
    command:
      - "--providers.docker=true"
      - "--entrypoints.web.address=:443"
    ports:
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
```

---

### 4. Gérer les secrets de manière sécurisée

`.env.example` contient des mots de passe en clair. Bien que `.env` ne soit
pas versionné, il faut aller plus loin :

| Risque | Solution |
|---|---|
| Mot de passe Keycloak en dur | Docker Secrets ou HashiCorp Vault |
| `POSTGRES_PASSWORD` dans `.env` | Variable d'environnement injectée au runtime |
| JWT secrets | Jamais en dur — utiliser des clés asymmetric RSA |

```yaml
# Utiliser Docker secrets au lieu de variables d'environnement
services:
  backend:
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
secrets:
  db_password:
    file: ./secrets/db_password.txt
```

---

## 🟠 Priorité haute — Architecture

### 5. Ajouter un gestionnaire d'erreurs global (Frontend)

Chaque composant gère les erreurs individuellement avec
`error => console.log(error)`. Cela mène à une gestion incohérente.

**Solution :** Créer un `ErrorHandler` Angular centralisé :

```typescript
// app/error-handler.ts
import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { NotificationService } from './_service/notification.service';

@Injectable()
export class AppErrorHandler implements ErrorHandler {
  constructor(private injector: Injector) {}

  handleError(error: any): void {
    const notification = this.injector.get(NotificationService);

    if (error.status === 401) {
      notification.showError('Session expirée. Veuillez vous reconnecter.');
    } else if (error.status === 403) {
      notification.showError('Accès interdit.');
    } else {
      notification.showError('Une erreur est survenue. Réessayez.');
    }

    console.error('Unhandled error:', error);
  }
}
```

```typescript
// app.module.ts
providers: [
  { provide: ErrorHandler, useClass: AppErrorHandler },
]
```

---

### ~~6. Introduire des DTOs pour l'API (Backend)~~

Les contrôleurs retournent directement les entités JPA (`Books`, `Users`).
Cela expose le schéma interne de la base (colonnes, relations Hibernate) et
surtout le **mot de passe hashé** via l'endpoint `/admin/users`.

**Avant (risqué) :**

```java
@GetMapping("/users")
public List<Users> getAllUsers() {
    return usersService.getAllUsers();
    // ← Le JSON contient le champ "password" (hash BCrypt)
}
```

**Après (sécurisé) :**

```java
// DTO de réponse — jamais de mot de passe
public record UserResponse(
    long userId,
    String username,
    String name,
    List<String> roles
) {}

@GetMapping("/users")
public List<UserResponse> getAllUsers() {
    return usersService.getAllUsers().stream()
        .map(u -> new UserResponse(
            u.getUserId(),
            u.getUsername(),
            u.getName(),
            u.getRole().stream().map(Role::getRoleName).toList()
        ))
        .toList();
}
```

> **Extensions recommandées :**
> - [MapStruct](https://mapstruct.org/) pour mapper automatiquement
>   Entity → DTO.
> - [SpringDoc OpenAPI](https://springdoc.org/) avec annotations
>   `@Operation` et `@Tag` pour la documentation Swagger.

---

### ~~7. Remplacer `console.log` par un logging structuré (Backend)~~

Les contrôleurs ne loguent pas les requêtes. Les composants de sécurité
(`SecurityAuditFilter`, `JwtAuthenticationEntryPoint`) le font bien.
Étendre cette pratique à toute la couche controller.

```java
@RestController
@Slf4j  // Lombok génère : private static final Logger log = ...
public class BooksController {

    @GetMapping("/books")
    public List<Books> getAllBooks() {
        log.info("Requête GET /admin/books — {} livres en base",
                 booksRepository.count());
        return booksService.getAllBooks();
    }
}
```

---

### ~~8. Ajouter la pagination~~

`GET /admin/books` et `GET /admin/users` retournent **tous** les enregistrements.
Pour une bibliothèque réelle avec des centaines de livres et utilisateurs,
c'est un problème de performance et de sécurité.

**Backend (Spring Data) :**
B
```java
@GetMapping("/books")
public Page<Books> getAllBooks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return booksRepository.findAll(PageRequest.of(page, size));
}
```

**Frontend (Angular) :**

```typescript
getBooks(page: number = 0, size: number = 20): Observable<Page<Books>> {
  return this.httpClient.get<Page<Books>>(
    `${apiUrl()}/admin/books?page=${page}&size=${size}`
  );
}
```

---

## 🟡 Priorité moyenne — Qualité du code

### ~~9. Nettoyer les modèles frontend~~

La classe `Users` expose le champ `password` côté client. C'est un risque
de sécurité ( même hashé, le hash ne doit jamais quitter le backend ).

**Fichiers concernés :** `src/app/_model/users.ts`

```typescript
// Avant — le password est envoyé au navigateur
export class Users {
  userId: number;
  username: string;
  name: string;
  password: string;  // ← à supprimer
  role: { roleName: string }[];
}

// Après — types séparés pour chaque contexte
export interface UserListItem {
  userId: number;
  username: string;
  name: string;
  roles: string[];
}

export interface CreateUserRequest {
  username: string;
  name: string;
  password: string;
  roles: string[];
}
```

---

### 10. Utiliser `async/await` ou `switchMap` au lieu de `subscribe` imbriqués

Plusieurs composants imbriquent des appels réseau. Cela rend le code
difficile à lire et à maintenir.

**Avant :**

```typescript
ngOnInit(): void {
  this.id = +this.route.snapshot.params['userId'];
  this.userService.getUserById(this.id).subscribe(data => {
    this.user = data;
    this.borrowService.getBooksBorrowedByUser(this.user.userId).subscribe(borrows => {
      this.borrow = borrows;
    });
  });
}
```

**Après :**

```typescript
ngOnInit(): void {
  this.id = +this.route.snapshot.params['userId'];
  this.userService.getUserById(this.id).pipe(
    switchMap(user => {
      this.user = user;
      return this.borrowService.getBooksBorrowedByUser(user.userId);
    })
  ).subscribe(borrows => {
    this.borrow = borrows;
  });
}
```

---

### 11. Ajouter la validation des entrées (Backend)

Les contrôleurs acceptent du JSON brut sans validation. Un client malveillant
peut envoyer un livre avec un nom vide ou un utilisateur sans username.

```java
// Entity — ajouter les annotations Jakarta Validation
@Entity
public class Books {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @NotBlank(message = "Le nom du livre est obligatoire")
    @Size(max = 255)
    private String bookName;

    @NotBlank(message = "L'auteur est obligatoire")
    private String bookAuthor;

    @Min(value = 0, message = "Le nombre de copies ne peut pas être négatif")
    private int noOfCopies;
}

// Controller — ajouter @Valid
@PostMapping("/books")
public Books createBook(@Valid @RequestBody Books book) {
    return booksService.createBook(book);
}
```

---

### ~~12. Uniformiser la langue des tests~~

Les descriptions des tests mélangent français et anglais :

```typescript
// Anglais
it('should create', () => { ... });

// Français
it('charge la liste des livres au démarrage', () => { ... });
it('survit à une sérialisation JSON', () => { ... });
```

**Recommandation :** Choisir une seule langue (français pour ce projet
pédagogique) et l'appliquer uniformément.

---

## 🟢 Priorité basse — Nice-to-have

### 13. Compléter la documentation API (Swagger)

`springdoc-openapi-ui` est déjà dans le `pom.xml` mais les contrôleurs
n'ont aucune annotation Swagger.

```java
@RestController
@RequestMapping("/admin/books")
@Tag(name = "Livres", description = "CRUD des livres (admin)")
public class BooksController {

    @GetMapping
    @Operation(summary = "Lister tous les livres",
               description = "Retourne la liste complète des livres.")
    public List<Books> getAllBooks() { ... }

    @PostMapping
    @Operation(summary = "Créer un livre")
    @PreAuthorize("hasRole('Admin')")
    public Books createBook(@RequestBody Books book) { ... }
}
```

> Accessible sur `/swagger-ui/index.html` une fois le backend démarré.

---

### ~~14. Mettre en place un pipeline CI/CD~~

Aucun fichier `.github/workflows` n'existe. Un pipeline minimal :

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, 'feat/**']
  pull_request:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: cd bibliotheque-backend && mvn -B test

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: bibliotheque-frontend/package-lock.json
      - run: cd bibliotheque-frontend && npm ci
      - run: cd bibliotheque-frontend && npx ng test --no-watch --browsers=ChromeHeadless
```

---

### ~~15. Ajouter un test E2E avec Keycloak~~

Les tests backend mockent le décodeur JWT. Un test d'intégration complet
obtiendrait un vrai token depuis Keycloak :

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndAuthTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void emprunter_avec_token_valide() {
        // 1. Obtenir un token depuis Keycloak
        String token = obtainToken("A1", "A1123");

        // 2. Appeler l'API avec le token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(
            "/borrow", HttpMethod.POST,
            new HttpEntity<>(Map.of("bookId", 1, "userId", 1), headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

### ~~16. Activer `OnPush` change detection~~

Tous les composants Angular utilisent la détection de changement par défaut
(`Default`). Pour les listes de livres, utilisateurs et emprunts, passer à
`OnPush` améliore significativement les performances :

```typescript
@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BooksListComponent implements OnInit {
  books: Books[] = [];

  constructor(private booksService: BooksService) {}

  ngOnInit(): void {
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
    });
  }
}
```

---

## 🔴 Priorité critique — Sécurité & fiabilité (nouvelles recommandations)

### 17. Réactiver `@PreAuthorize` sur la création d'utilisateurs

**Fichier :** `AdminController.java:42`

L'annotation `@PreAuthorize("hasRole('Admin')")` est commentée sur
`addUserByAdmin()`. N'importe qui (même non authentifié) peut créer
des comptes admin.

```java
// Avant — n'importe qui peut créer un admin
@PostMapping("/users")
//  @PreAuthorize("hasRole('Admin')")
public Users addUserByAdmin(@Valid @RequestBody UserCreateRequest request) { ... }

// Après
@PostMapping("/users")
@PreAuthorize("hasRole('Admin')")
public Users addUserByAdmin(@Valid @RequestBody UserCreateRequest request) { ... }
```

---

### 18. Sécuriser le BorrowController

**Fichier :** `BorrowController.java`

Le contrôleur d'emprunt n'a aucune annotation `@PreAuthorize`. De plus,
`/borrow/**` est dans la liste `permitAll()` de `WebSecurityConfiguration`.
Toute personne peut emprunter, retourner, et voir tous les emprunts.

```java
@RestController
@RequestMapping("/borrow")
@Slf4j
public class BorrowController {

    @PreAuthorize("hasAnyRole('User', 'Admin')")
    @PostMapping
    public ResponseEntity<?> borrowBook(@Valid @RequestBody Borrow request) { ... }

    @PreAuthorize("hasAnyRole('User', 'Admin')")
    @PutMapping
    public ResponseEntity<?> returnBook(@Valid @RequestBody Borrow request) { ... }
}
```

> **Note :** Retirer `/borrow/**` de `permitAll()` dans
> `WebSecurityConfiguration`.

---

### 19. Externaliser la clé secrète JWT

**Fichier :** `JwtUtil.java:17`

La clé de signature JWT est en dur dans le code :
`"learn_programming_yourself"`. C'est une faille critique.

```java
// Avant
private static final String SECRET_KEY = "learn_programming_yourself";

// Après — injecter depuis application.properties
@Value("${jwt.secret}")
private String secretKey;
```

```properties
# application.properties
jwt.secret=${JWT_SECRET:clé-par-défaut-seulement-en-dev}
```

---

### ~~20. Remplacer `Optional.get()` par `orElseThrow()`~~

**Fichiers :** `BorrowController.java` (lignes 33, 34, 62, 63),
`JwtService.java` (lignes 42, 48)

`Optional.get()` lève `NoSuchElementException` (500) si l'élément
n'existe pas. Utiliser `orElseThrow()` avec une exception métier.

```java
// Avant — crash si pas trouvé
usersRepository.findById(borrow.getUserId()).get();

// Après — erreur 404 propre
usersRepository.findById(borrow.getUserId())
    .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
```

---

### 21. Supprimer les identifiants en dur dans `application.properties`

**Fichier :** `application.properties:4-5`

```properties
# Avant — credentials en dur
spring.datasource.username=postgres
spring.datasource.password=postgres

# Après — variables d'environnement uniquement
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
```

---

## 🟠 Priorité haute — Architecture (nouvelles recommandations)

### ~~22. Ajouter une couche Service pour Books et Borrow~~

**Fichiers :** `BooksController.java`, `BorrowController.java`

Les contrôleurs injectent directement les repositories. Cela viole
l'architecture en couches et rend le code difficile à tester.

```java
// Créer BooksService
@Service
@Slf4j
public class BooksService {
    private final BooksRepository booksRepository;

    public BooksService(BooksRepository booksRepository) {
        this.booksRepository = booksRepository;
    }

    public Page<Books> getAllBooks(Pageable pageable) {
        return booksRepository.findAll(pageable);
    }
    // ...
}

// Utiliser dans le contrôleur
@RestController
public class BooksController {
    private final BooksService booksService;

    public BooksController(BooksService booksService) {
        this.booksService = booksService;
    }
}
```

---

### ~~23. Injection par constructeur au lieu de `@Autowired`~~

**Fichiers :** Tous les contrôleurs et services

L'injection par champ (`@Autowired` sur le champ) est déconseillée.
L'injection par constructeur est testable et immutable.

```java
// Avant
@Autowired
private BooksRepository booksRepository;

// Après
private final BooksRepository booksRepository;

public BooksController(BooksRepository booksRepository) {
    this.booksRepository = booksRepository;
}
```

---

### ~~24. Ajouter `@ControllerAdvice` pour la gestion globale des erreurs~~

**Fichier :** `GlobalExceptionHandler.java` (nouveau)

Aucun `@ControllerAdvice` n'existe. Les exceptions retournent des
réponses 500 sans structure.

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : " invalide"
            ));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(400, "Erreur de validation", errors));
    }
}
```

---

### 25. Supprimer les `@CrossOrigin` en dur sur les contrôleurs

**Fichiers :** Tous les contrôleurs

`@CrossOrigin("http://localhost:4200/")` est dupliqué sur chaque
contrôleur alors qu'une `CorsConfiguration` centralisée existe déjà.

```java
// Supprimer de chaque contrôleur
// @CrossOrigin("http://localhost:4200/")  ← supprimer

// Garder uniquement CorsConfiguration.java avec :
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins("http://localhost:4200")
        .allowedMethods("GET", "POST", "PUT", "DELETE");
}
```

---

### 26. Corriger `@Repository` sur BorrowController

**Fichier :** `BorrowController.java:17`

`@Repository` est une annotation pour les composants d'accès aux
données, pas pour les contrôleurs.

```java
// Avant
@Repository
@RestController
public class BorrowController { ... }

// Après
@RestController
@RequestMapping("/borrow")
public class BorrowController { ... }
```

---

### 27. Nettoyer le code mort dans BorrowController

**Fichier :** `BorrowController.java:84-183`

Plus de 100 lignes de code commenté (3 implémentations différentes)
doivent être supprimées. L'historique Git préserve déjà ces versions.

---

### ~~28. Ajouter le nettoyage des subscriptions RxJS~~

**Fichiers :** Tous les composants avec `.subscribe()`

Aucun composant n'implémente `OnDestroy` pour nettoyer les abonnements.
Cela cause des fuites mémoire si le composant est détruit avant
l'achèvement de l'Observable.

```typescript
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({...})
export class BooksListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.booksService.getBooksList()
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.books = data);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

### 29. Ajouter des gestionnaires d'erreur dans les subscriptions

**Fichiers :** 13+ appels `.subscribe()` sans callback d'erreur

La plupart des appels réseau n'ont pas de gestionnaire d'erreur.
Les erreurs sont silencieusement ignorées.

```typescript
// Avant
this.booksService.getBooksList().subscribe(data => {
  this.books = data;
});

// Après
this.booksService.getBooksList().subscribe({
  next: (data) => this.books = data,
  error: (err) => this.notificationService.showError('Erreur de chargement')
});
```

---

## 🟡 Priorité moyenne — Docker & Déploiement (nouvelles recommandations)

### 30. Ajouter des `restart` policies aux services Docker

**Fichier :** `docker-compose.yml`

Les containers ne redémarrent pas automatiquement après un crash ou
un redémarrage de l'hôte.

```yaml
services:
  backend:
    restart: unless-stopped
  frontend:
    restart: unless-stopped
  keycloak:
    restart: unless-stopped
  db:
    restart: unless-stopped
```

---

### 31. Ajouter des limites de ressources aux containers

**Fichier :** `docker-compose.yml`

Un processus déréglé peut épuiser toutes les ressources de l'hôte.

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
```

---

### 32. Ajouter un healthcheck au backend

**Fichier :** `docker-compose.yml`

Le `depends_on` n'attend que le démarrage du container, pas sa
disponibilité.

```yaml
backend:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
```

---

### 33. Rendre Caddy optionnel avec un profile Docker

**Fichier :** `docker-compose.yml`

Caddy est défini sans profile, donc `docker compose up` le démarre
toujours. Utiliser un profile pour le rendre optionnel.

```yaml
services:
  caddy:
    image: caddy:2-alpine
    profiles: ["https"]
    ...

# Utilisation :
# docker compose up                    ← sans HTTPS
# docker compose --profile https up    ← avec HTTPS
```

---

### 34. Rendre les ports configurables via `.env`

**Fichier :** `docker-compose.yml`

Les ports sont en dur (5435, 8080, 4200, 8081). Les rendre
configurables via `.env`.

```yaml
ports:
  - "${POSTGRES_PORT:-5435}:5432"
  - "${BACKEND_PORT:-8080}:8080"
  - "${FRONTEND_PORT:-4200}:80"
  - "${KEYCLOAK_PORT:-8081}:8080"
```

---

## 🟡 Priorité moyenne — Qualité du code (nouvelles recommandations)

### ~~35. Ajouter la validation côté client~~

**Fichiers :** Tous les formulaires Angular

Les formulaires n'ont aucune validation côté client. Des formulaires
vides peuvent être soumis.

```html
<!-- Avant -->
<input type="text" [(ngModel)]="user.username">

<!-- Après -->
<input type="text" [(ngModel)]="user.username" required #username="ngModel">
<div *ngIf="username.invalid && username.touched" class="error">
  Le nom d'utilisateur est obligatoire.
</div>
```

---

### 36. Ajouter une confirmation avant suppression

**Fichier :** `books-list.component.ts:33`

La suppression de livre se fait sans confirmation. `ReservationListComponent`
utilise déjà `window.confirm()`.

```typescript
// Avant
deleteBook(bookId: number) {
  this.booksService.deleteBook(bookId).subscribe(() => this.getBooks());
}

// Après
deleteBook(bookId: number) {
  if (window.confirm('Êtes-vous sûr de vouloir supprimer ce livre ?')) {
    this.booksService.deleteBook(bookId).subscribe(() => this.getBooks());
  }
}
```

---

### ~~37. Ajouter des headers de sécurité dans nginx~~

**Fichier :** `bibliotheque-frontend/nginx.conf`

Aucun header de sécurité n'est configuré dans nginx.

```nginx
server {
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'" always;
}
```

---

### 38. Désactiver `show-sql` en production

**Fichier :** `application.properties:8`

`spring.jpa.show-sql=true` logue le SQL dans stdout, ce qui peut
exposer des données sensibles.

```properties
# Utiliser un profile pour la production
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}

# application-dev.properties
spring.jpa.show-sql=true

# application-prod.properties
spring.jpa.show-sql=false
```

---

### 39. Remplacer `ddl-auto=update` par Flyway

**Fichier :** `application.properties:10`

`spring.jpa.hibernate.ddl-auto=update` peut modifier silencieusement
le schéma de production.

```properties
# Avant
spring.jpa.hibernate.ddl-auto=update

# Après — utiliser Flyway pour les migrations
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

### 40. Corriger `CascadeType.ALL` sur les rôles utilisateur

**Fichier :** `Users.java:28`

`CascadeType.ALL` sur `@ManyToMany` pour les rôles signifie que
supprimer un utilisateur cascade-supprime les rôles, affectant les
autres utilisateurs.

```java
// Avant
@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)

// Après
@ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
```

---

## 🟢 Priorité basse — Nice-to-have (nouvelles recommandations)

### ~~41. Ajouter des tests unitaires pour les contrôleurs~~

**Fichiers :** `BooksController.java`, `BorrowController.java`,
`AdminController.java`, `MeController.java`

Aucun test unitaire n'existe pour les contrôleurs. Seuls des tests
d'intégration avec MockMvc sont présents.

---

### ~~42. Ajouter la couverture de code dans le CI~~

**Fichiers :** `pom.xml`, `.github/workflows/ci.yml`

Aucun JaCoCo pour le backend, aucun reporter de couverture pour le
frontend. Le CI ne vérifie pas le minimum de couverture.

```yaml
# Ajouter au pipeline CI
- name: Run tests with coverage
  run: cd bibliotheque-backend && mvn -B test jacoco:report
```

---

### ~~43. Corriger les incohérences de nommage des entités~~

**Fichiers :** Toutes les entités

Le code mélange français et anglais : `Books`, `Users`, `Borrow`
(anglais) vs `Reservation`, `StatutReservation` (français). Les noms
de tables sont aussi incohérents (`Books` vs `reservation`).

---

### 44. Standardiser la stratégie de génération d'IDs

**Fichiers :** `Books.java`, `Users.java`, `Borrow.java`,
`Reservation.java`

`Books` et `Users` utilisent `GenerationType.AUTO`, tandis que
`Borrow` et `Reservation` utilisent `GenerationType.IDENTITY`.

---

## Résumé — Plan d'action complet

| Priorité | Action | Effort | Impact |
|---|---|---|---|
| 🔴 | ~~Supprimer `console.log` frontend~~ | Faible | Sécurité |
| 🔴 | ~~Ajouter HTTPS (reverse proxy)~~ | Moyen | Sécurité |
| 🔴 | ~~Réactiver `@PreAuthorize` sur addUser~~ | Faible | Sécurité |
| 🔴 | ~~Sécuriser BorrowController~~ | Faible | Sécurité |
| 🔴 | ~~Externaliser clé JWT~~ | Faible | Sécurité |
| 🔴 | ~~Remplacer `Optional.get()`~~ | Faible | Fiabilité |
| 🔴 | ~~Supprimer credentials en dur~~ | Faible | Sécurité |
| 🔴 | ~~Supprimer valeur par défaut JWT~~ | Faible | Sécurité |
| 🔴 | ~~Corriger `getUserData()` async~~ | Faible | Fiabilité |
| 🟠 | ~~ErrorHandler global Angular~~ | Moyen | UX / Maintenance |
| 🟠 | ~~DTOs pour l'API backend~~ | Moyen | Sécurité / API |
| 🟠 | ~~Pagination~~ | Moyen | Performance |
| 🟠 | ~~Couche Service pour Books/Borrow~~ | Moyen | Architecture |
| 🟠 | ~~Injection par constructeur~~ | Moyen | Testabilité |
| 🟠 | ~~`@ControllerAdvice` global~~ | Moyen | Maintenance |
| 🟠 | ~~Supprimer `@CrossOrigin` en dur~~ | Faible | Cohérence |
| 🟠 | ~~Nettoyage RxJS subscriptions~~ | Moyen | Fiabilité |
| 🟠 | ~~Gestionnaires d'erreur subscriptions~~ | Faible | Robustesse |
| 🟠 | ~~Ajouter `@Valid` sur BorrowController~~ | Faible | Robustesse |
| 🟠 | ~~Ajouter validation sur entité Borrow~~ | Faible | Robustesse |
| 🟠 | ~~Limiter CORS en production~~ | Faible | Sécurité |
| 🟠 | ~~`cap_drop` + `no-new-privileges` Docker~~ | Faible | Sécurité |
| 🟠 | ~~Tests unitaires BooksService/BorrowService~~ | Moyen | Qualité |
| 🟡 | ~~Validation des entrées (`@Valid`)~~ | Faible | Robustesse |
| 🟡 | ~~`switchMap` au lieu de `subscribe` imbriqués~~ | Faible | Lisibilité |
| 🟡 | ~~`restart` policies Docker~~ | Faible | Disponibilité |
| 🟡 | ~~Limites de ressources containers~~ | Faible | Stabilité |
| 🟡 | ~~Healthcheck backend~~ | Faible | Disponibilité |
| 🟡 | ~~Validation côté client Angular~~ | Faible | UX |
| 🟡 | ~~Headers sécurité nginx~~ | Faible | Sécurité |
| 🟡 | ~~Désactiver `show-sql`~~ | Faible | Sécurité |
| 🟡 | ~~Remplacer `ddl-auto` par Flyway~~ | Moyen | Fiabilité |
| 🟡 | ~~Corriger raw type `Set` dans JwtService~~ | Faible | Type safety |
| 🟡 | ~~Masquer stack traces en production~~ | Faible | Sécurité |
| 🟡 | ~~Scope `provided` pour devtools~~ | Faible | Sécurité |
| 🟡 | ~~Types `any` → types stricts~~ | Moyen | Type safety |
| 🟡 | ~~Assertions non-null localStorage~~ | Faible | Fiabilité |
| 🟡 | ~~`.dockerignore` restrictif~~ | Faible | Sécurité |
| 🟡 | ~~Avertissement .env production~~ | Faible | Sécurité |
| 🟡 | ~~Activer Actuator métriques~~ | Faible | Observabilité |
| 🟡 | ~~DTO pour `GET /admin/books` paginé~~ | Moyen | Sécurité |
| 🟢 | ~~Pipeline CI/CD~~ | Moyen | Fiabilité |
| 🟢 | ~~Swagger annotations~~ | Faible | Documentation |
| 🟢 | ~~Tests unitaires contrôleurs~~ | Moyen | Qualité |
| 🟢 | ~~Couverture de code CI~~ | Moyen | Qualité |
| 🟢 | ~~OnPush change detection~~ | Faible | Performance |
| 🟢 | ~~Image Alpine au lieu de Jammy~~ | Faible | Taille image |
| 🟢 | ~~HEALTHCHECK dans Dockerfile frontend~~ | Faible | Portabilité |
| 🟢 | ~~Supprimer `@EntityListeners` inutile~~ | Faible | Performance |
| 🟢 | ~~Logging dans scheduled task~~ | Faible | Observabilité |
| 🟢 | ~~Fix `as any` dates~~ | Faible | Type safety |
| 🟢 | ~~Initialisation champ header~~ | Faible | Fiabilité |
| 🟡 | ~~Langue unique (français) pour les tests~~ | Faible | Lisibilité |
| 🟡 | ~~Content-Security-Policy nginx~~ | Faible | Sécurité |
| 🟡 | ~~Seuils de couverture bloquants (JaCoCo, Karma)~~ | Faible | Qualité |
| 🟢 | ~~Test E2E avec un vrai Keycloak~~ | Élevé | Qualité |
| 🟢 | ~~Nommage des entités (`ReservationStatus`)~~ | Moyen | Lisibilité |
| 🟠 | Percentiles de latence et SLO (73) | Faible | Performance |
| 🟠 | Test de charge k6 (74) | Moyen | Performance |
| 🟠 | Quota CPU du backend (75) | Faible | Performance |
| 🟡 | Heap JVM et GC (76) | Faible | Performance |
| 🟡 | Threads, connexions, `open-in-view` (77) | Faible | Performance |
| 🟡 | Compression et cache nginx (78) | Faible | Performance |
| 🟢 | Pas de `count()` pour un log (79) | Faible | Performance |
| 🟠 | Suppression du N+1 des rôles (80) | Moyen | Performance |
| 🟠 | Index manquants (81) | Faible | Performance |
| 🔴 | Décrémentation atomique du stock (82) | Faible | Fiabilité |
| 🟢 | Expiration des réservations en une requête (83) | Faible | Performance |
| 🟡 | Jointures côté serveur (84) | Moyen | Performance |
| 🟢 | Chargement paresseux des écrans admin (85) | Moyen | Performance |

---

## Nouvelles recommandations (45-72)

### 45. Supprimer la valeur par défaut du secret JWT
**Statut :** ~~Fait~~ — `JwtUtil.java` utilise maintenant `@Value("${jwt.secret}")` sans fallback.

### 46. Ajouter `@Valid` sur BorrowController
**Statut :** ~~Fait~~ — `@Valid @RequestBody` ajouté sur `borrowBook()` et `returnBook()`.

### 47. Ajouter validation sur entité Borrow
**Statut :** ~~Fait~~ — `@NotNull` ajouté sur `bookId` et `userId`. `@EntityListeners` inutile supprimé.

### 48. Corriger raw type dans JwtService
**Statut :** ~~Fait~~ — `Set` → `Set<SimpleGrantedAuthority>` dans `getAuthority()`.

### 49. Limiter CORS en production
**Statut :** ~~Fait~~ — `allowedOriginPatterns("*")` remplacé par `${CORS_ALLOWED_ORIGINS}`.

### 50. DTO pour GET /admin/books paginé
**Statut :** ~~Fait~~ — `BookResponse` DTO créé, `BooksController` mis à jour.

### 51. Supprimer `@EntityListeners` inutile
**Statut :** ~~Fait~~ — Supprimé de l'entité `Borrow`.

### 52. `fixedDelay` au lieu de `fixedRate`
**Statut :** ~~Fait~~ — `@Scheduled(fixedDelayString = "60000")`.

### 53. Logging dans scheduled task
**Statut :** ~~Fait~~ — `log.info("Expiration de {} réservation(s) dépassées")`.

### 54. Masquer stack traces en production
**Statut :** ~~Fait~~ — `log.error("Erreur inattendue: {}", ex.getMessage())`.

### 55. Scope `provided` pour devtools
**Statut :** ~~Fait~~ — `<scope>provided</scope>` dans `pom.xml`.

### 56. Error handlers manquants
**Statut :** ~~Fait~~ — 17 subscriptions corrigées dans 10 composants.

### 57. Corriger `getUserData()` async
**Statut :** ~~Fait~~ — Retourne `''` si pas encore chargé, charge en arrière-plan.

### 58. Types `any` → types stricts
**Statut :** ~~Fait~~ — 14 occurrences corrigées (users, reservation, login, error-handler).

### 59. Assertions non-null localStorage
**Statut :** ~~Fait~~ — Types de retour `| null` avec vérification explicite.

### 60. Fix `as any` dates
**Statut :** ~~Fait~~ — `new Date(b.issueDate).toLocaleDateString()`.

### 61. Types dédiés pour env vars
**Statut :** ~~Fait~~ — `env.d.ts` créé avec interface `EnvConfig` et `declare global`.

### 62. Initialisation champ header
**Statut :** ~~Fait~~ — `name` initialisé dans le constructeur.

### 63. Image Alpine au lieu de Jammy
**Statut :** ~~Fait~~ — `eclipse-temurin:8-jre-alpine`.

### 64. `.dockerignore` restrictif
**Statut :** ~~Fait~~ — Ajout de `.angular`, `.cache`, `coverage`.

### 65. HEALTHCHECK dans Dockerfile frontend
**Statut :** ~~Fait~~ — `HEALTHCHECK --interval=10s --timeout=5s --retries=5`.

### 66. `cap_drop` + `no-new-privileges`
**Statut :** ~~Fait~~ — Ajouté à tous les services Docker, y compris `caddy` (profile `https`) avec `cap_add: NET_BIND_SERVICE`, limites et healthcheck.

### 67. Avertissement .env production
**Statut :** ~~Fait~~ — En-tête d'avertissement ajouté à `.env.example`.

### 68. Activer Actuator métriques
**Statut :** ~~Fait~~ — `management.endpoints.web.exposure.include=health,info,metrics,prometheus` et dépendance `micrometer-registry-prometheus` (sans elle `/actuator/prometheus` répondait 404).

### 69. Header corrélation requestId
**Statut :** ~~Fait~~ — `X-Request-ID` généré côté frontend, placé dans le MDC par `SecurityAuditFilter` et affiché sur chaque ligne de log grâce à `logging.pattern.level` (`[requestId=…]`).

### 70. Tests unitaires BooksService/BorrowService
**Statut :** ~~Fait~~ — `BooksServiceTest` (9 tests) et `BorrowServiceTest` (11 tests) créés.

### 71. Tests Angular réels
**Statut :** ~~Fait~~ — Les tests existants ont été mis à jour avec les corrections de types.

### 72. Test flow emprunt→retour complet
**Statut :** ~~Fait~~ — Couvert par `BorrowServiceTest.returnBook_empruntExistant_rendLeLivre` (et les cas d'erreur associés), et de bout en bout par `KeycloakEndToEndIT.adherent_empruntePuisRendUnLivre`.

---

## ⚡ Performance (recommandations 73-85)

> Chapitre fondé sur une taxonomie de la performance des architectures
> logicielles (dimensions mesurables → anti-patterns → goulots
> d'étranglement → optimisations) appliquée à cette application. Les
> chiffres de cette taxonomie sont indicatifs ; **tous les chiffres
> ci-dessous ont été mesurés sur la stack Docker du projet** (14/09/2026).
>
> Architecture actuelle : **monolithe en couches** (contrôleur → service →
> repository). C'est suffisant pour ce projet : on optimise les ressources
> partagées avant d'envisager un changement d'architecture.

### Mesures de référence

40 requêtes GET séquentielles par endpoint (jeton admin, 5 premières
ignorées). Un seul client : c'est une référence, pas un test de charge.

| Endpoint | p50 | p95 | max |
|---|---|---|---|
| `GET /admin/books?size=20` | 11,0 ms | 93,5 ms | 203,6 ms |
| `GET /admin/users?size=20` | 13,8 ms | 95,0 ms | 99,1 ms |
| `GET /borrow` | 3,8 ms | 82,6 ms | 91,7 ms |
| `GET /api/reservations` | 10,4 ms | 93,4 ms | 98,7 ms |
| `GET /profile` | 7,7 ms | 82,7 ms | 89,2 ms |

| Autre mesure | Valeur |
|---|---|
| Périodes CPU bridées pendant une rafale de 60 requêtes | 21 sur 23 (2,56 s bridées) |
| Lectures de tables pour un `GET /api/reservations` (3 lignes) | 7 |
| Lectures de tables pour un `GET /borrow` | 9 |
| Heap JVM max (conteneur 512 Mo) | 128 Mo, collecteur Serial |
| Threads Tomcat / connexions base | 200 / 10 |
| `main.js` servi / possible en gzip | 367 Ko / 100 Ko |
| Feuille de style servie / possible en gzip | 120 Ko / 18 Ko |
| Histogrammes de latence exportés | aucun |

Les recommandations sont **numérotées dans l'ordre conseillé** : mesurer,
supprimer les goulots partagés, corriger l'accès aux données, puis alléger
le navigateur. Refaire les mesures après chaque groupe.

### 73. Exporter les percentiles de latence et un SLO

**Taxonomie :** Latence → distribution des percentiles, SLO/SLA
**Fichier :** `application.properties`

Prometheus n'expose aucune série `http_server_requests_seconds_bucket` :
p95 et p99 sont impossibles à suivre, donc aucune optimisation ne peut
être prouvée.

```properties
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.slo.http.server.requests=50ms,100ms,200ms,500ms
```

---

### 74. Ajouter un test de charge reproductible

**Taxonomie :** Débit → courbe de réponse à la charge, point de saturation
**Fichiers :** `perf/load-test.js` (nouveau), CI (optionnel)

Aucun test de charge n'existe. Un script k6 qui monte progressivement en
utilisateurs sur les vrais endpoints permet de trouver le « coude » (charge
où la latence cesse d'être linéaire) et de comparer chaque changement.

```javascript
export const options = {
  stages: [{ duration: '1m', target: 10 }, { duration: '2m', target: 50 }, { duration: '1m', target: 100 }],
  thresholds: { http_req_duration: ['p(95)<50', 'p(99)<100'], http_req_failed: ['rate<0.001'] },
};
```

---

### 75. Donner assez de CPU au backend

**Taxonomie :** Anti-pattern « bridage CPU (limites de conteneur) » → goulot CPU
**Fichier :** `docker-compose.yml`

`cpus: '0.5'` autorise 50 ms de CPU par période de 100 ms
(`cpu.max 50000 100000`). Une fois ce budget consommé, le noyau suspend la
JVM jusqu'à la période suivante : c'est la bande de 83–95 ms observée au
p95 sur **tous** les endpoints.

```yaml
# Avant
backend:
  deploy:
    resources:
      limits:
        cpus: '0.5'

# Après — puis re-mesurer (lire /sys/fs/cgroup/cpu.stat avant/après)
backend:
  deploy:
    resources:
      limits:
        cpus: '2'
```

---

### 76. Dimensionner la JVM pour son conteneur

**Taxonomie :** Empreinte mémoire → réglage du GC
**Fichier :** `docker-compose.yml`

Java 8 réserve par défaut 25 % de la RAM au heap : 128 Mo sur 512 Mo, avec
le collecteur Serial (pauses « stop-the-world »). Choisir le collecteur
après la recommandation 75, qui change le nombre de CPU vus par la JVM.

```yaml
backend:
  environment:
    JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=65 -XX:+UseG1GC -XX:MaxGCPauseMillis=50"
```

---

### 77. Aligner threads, connexions et file d'attente

**Taxonomie :** Concurrence → réglage des pools ; anti-pattern « pas de backpressure » ; cycle de vie « pool pré-chauffé »
**Fichier :** `application.properties`

200 threads Tomcat se partagent 10 connexions : 190 ne peuvent qu'attendre,
jusqu'à 30 s (délai Hikari par défaut). `open-in-view` (actif par défaut)
garde en plus la connexion pendant toute la requête, sérialisation JSON
comprise.

Dimensionnement : `threads = cœurs × (1 + W/C)` (W = attente I/O,
C = calcul). Avec 2 cœurs et des requêtes qui attendent la base environ
4 fois plus longtemps qu'elles ne calculent : ~10 threads utiles.

```properties
server.tomcat.threads.max=40
server.tomcat.accept-count=100
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=5000
spring.datasource.hikari.leak-detection-threshold=60000
spring.jpa.open-in-view=false
```

Relancer toute la suite de tests après `open-in-view=false` : un chargement
paresseux hors transaction lèverait `LazyInitializationException`.

---

### 78. Compresser et mettre en cache les fichiers du frontend

**Taxonomie :** Latence → compression des réponses ; mise en cache du contenu statique
**Fichier :** `bibliotheque-frontend/nginx.conf`

nginx envoie tout sans compression (`main.js` 367 Ko au lieu de ~100 Ko,
CSS 120 Ko au lieu de ~18 Ko) et les fichiers hachés n'ont pas de durée de
cache.

```nginx
gzip on;
gzip_types application/javascript text/css application/json image/svg+xml;

# Fichiers dont le nom contient un hash : cache d'un an (index.html reste en no-cache)
location ~* \.[0-9a-f]{16}\.(js|css)$ {
    add_header Cache-Control "public, max-age=31536000, immutable" always;
}
```

> Attention : un `add_header` dans un `location` masque ceux du bloc
> `server` (en-têtes de sécurité, CSP) : les répéter ou les inclure dans ce
> bloc.

---

### 79. Ne pas interroger la base pour écrire un log

**Taxonomie :** Anti-pattern « journalisation sur le chemin critique »
**Fichier :** `AdminController.java`

```java
// Avant — une requête SQL de plus à chaque appel, juste pour le log
log.info("Requête GET /admin/users — {} utilisateurs en base", usersRepository.count());
return usersRepository.findAll(pageable).map(this::toUserResponse);

// Après — le total est déjà dans la page
Page<Users> page = usersRepository.findAll(pageable);
log.info("Requête GET /admin/users — {} utilisateurs en base", page.getTotalElements());
return page.map(this::toUserResponse);
```

---

### 80. Supprimer le chargement N+1 des rôles

**Taxonomie :** Anti-pattern « requêtes N+1 » / « appels bavards »
**Fichiers :** `Users.java`, `Reservation.java`, `ReservationRepository.java`

Un `GET /api/reservations` sur 3 réservations lit `role` 3 fois et
`user_role` 3 fois : `Users.role` est en `FetchType.EAGER` et les
`@ManyToOne` de `Reservation` sont chargés immédiatement par défaut. Le
nombre de requêtes croît avec le nombre de lignes.

```java
// Avant
@ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private Set<Role> role;

// Après — chargé seulement quand on en a besoin
@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private Set<Role> role;

// Et pour les listes : tout ce qu'elles affichent en une requête
@EntityGraph(attributePaths = {"livre", "adherent"})
List<Reservation> findByStatut(ReservationStatus statut);
```

Les autorisations lisent les rôles dans le jeton Keycloak : la plupart des
requêtes n'ont pas besoin des rôles en base. Ajouter un test qui vérifie
que le nombre de requêtes d'une liste ne dépend pas de sa taille.

---

### 81. Ajouter les index manquants

**Taxonomie :** Optimisation I/O → indexation et optimisation des requêtes
**Fichier :** `db/migration/V4__index_performance.sql` (nouveau)

Les migrations ne créent que des clés primaires. `users.username` est lu à
chaque requête authentifiée (`/profile`, contrôle de propriété des
emprunts) sans index ni contrainte d'unicité ; la tâche planifiée filtre les
réservations par statut et date toutes les 60 s.

```sql
CREATE UNIQUE INDEX ux_users_username ON users (username);
CREATE INDEX ix_borrow_user ON borrow (user_id);
CREATE INDEX ix_borrow_book ON borrow (book_id);
CREATE INDEX ix_reservation_adherent_statut ON reservation (adherent_id, statut);
CREATE INDEX ix_reservation_livre ON reservation (livre_id);
CREATE INDEX ix_reservation_statut_expiration ON reservation (statut, date_expiration);
```

---

### 82. Rendre la décrémentation du stock atomique

**Taxonomie :** Anti-pattern « état mutable partagé » → compare-and-swap
**Fichiers :** `BooksRepository.java`, `BorrowService.java`

`BorrowService` lit `no_of_copies`, le vérifie puis enregistre la nouvelle
valeur : deux adhérents qui empruntent le dernier exemplaire au même moment
peuvent réussir tous les deux (mise à jour perdue).

```java
// Avant — lecture, vérification, écriture
if (book.getNoOfCopies() < 1) { throw new BadRequestException(...); }
book.setNoOfCopies(book.getNoOfCopies() - 1);
booksRepository.save(book);

// Après — une seule instruction sérialisée par la base
@Modifying
@Query("update Books b set b.noOfCopies = b.noOfCopies - 1 where b.bookId = :id and b.noOfCopies > 0")
int takeOneCopy(@Param("id") Integer id);   // 0 ligne modifiée = plus d'exemplaire
```

---

### 83. Expirer les réservations en une seule requête

**Taxonomie :** Optimisation I/O → traitement par lots ; cycle de vie « portée transaction »
**Fichiers :** `ReservationRepository.java`, `ReservationService.java`

La tâche charge toutes les réservations dépassées, les modifie en mémoire
puis les enregistre une par une.

```java
@Modifying
@Query("update Reservation r set r.statut = 'EXPIREE' "
     + "where r.statut in :actifs and r.dateExpiration < :maintenant")
int expirerDepassees(@Param("actifs") Collection<ReservationStatus> actifs,
                     @Param("maintenant") LocalDateTime maintenant);
```

---

### 84. Faire les jointures côté serveur, pas dans le navigateur

**Taxonomie :** Anti-pattern « appels bavards » → optimisation d'algorithme
**Fichiers :** `borrow-list`, `borrow-book`, `return-book`, `reservations` (frontend), nouveaux endpoints (backend)

Ces écrans demandent jusqu'à 1 000 livres et 1 000 utilisateurs à chaque
affichage. `borrow-list` retrouve ensuite le livre et l'emprunteur de chaque
emprunt avec `Array.find` : emprunts × (livres + utilisateurs) comparaisons.

```typescript
// Avant — recherche linéaire pour chaque ligne
const bookName = (id: number) => books.find(b => b.bookId === id)?.bookName;

// Étape intermédiaire — index par identifiant, jointure linéaire
const livresParId = new Map(books.map(b => [b.bookId, b]));
const bookName = (id: number) => livresParId.get(id)?.bookName;
```

Cible : des endpoints paginés qui renvoient directement ce que chaque écran
affiche (emprunts avec titre et nom de l'emprunteur, livres disponibles).

---

### 85. Charger à la demande les écrans bibliothécaire

**Taxonomie :** Optimisation mémoire → chargement paresseux
**Fichier :** `app-routing.module.ts`

Les 15 routes sont déclarées d'emblée : chaque visiteur télécharge les
écrans d'administration dans un seul bundle de 367 Ko.

```typescript
// Avant
{ path: 'books', component: BooksListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },

// Après — module chargé seulement si l'on navigue vers l'administration
{ path: 'admin', loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule) },
```

---

### Non recommandé pour l'instant

| Idée | Pourquoi attendre |
|---|---|
| Cache distribué (Redis) | Les lectures sont petites et rapides ; mesurer après 80-81. Premier candidat si besoin : un cache local Caffeine `username → userId`. |
| Monolithe modulaire ou orienté événements | Un monolithe en couches suffit tant que les SLO sont tenus ; rien ici n'est limité par l'architecture. |
| Pile réactive ou threads virtuels | Les threads virtuels demandent Java 21 : à traiter avec la recommandation 2 (mise à jour des frameworks). |
| Journalisation asynchrone | Quelques lignes par action métier ; à revoir seulement si un profilage montre les logs. |

### Objectifs de performance

| Objectif | Vérification |
|---|---|
| Lectures API : p95 < 50 ms, p99 < 100 ms à 50 utilisateurs simultanés | Seuils k6 (74) et histogrammes Prometheus (73) |
| Taux d'erreur < 0,1 % pendant la montée en charge | Seuils k6 |
| Quasiment aucune période CPU bridée à la charge cible | `/sys/fs/cgroup/cpu.stat` avant/après |
| Nombre de requêtes SQL constant par liste | Test d'intégration (80) |
| Point de saturation connu et noté | Charge où le p95 double, consignée à chaque campagne |

---

## Corrections des éléments partiellement faits (14 septembre 2026)

Un audit du code a montré que 16 recommandations marquées « faites » ne
l'étaient qu'en partie, et que la 15 n'avait pas été commencée. Toutes sont
désormais terminées et vérifiées.

| # | Correction | Où | Vérifié par |
|---|---|---|---|
| 1 | Plus aucune sortie console en production : `AppErrorHandler` n'écrit qu'en mode développement (`isDevMode()`), et un échec de démarrage affiche un message au lieu d'une page blanche. | `error-handler.service.ts`, `main.ts` | Tests Karma |
| 6 | Plus aucune entité renvoyée par l'API : `BorrowResponse` (mêmes champs JSON), `addUserByAdmin` renvoie `UserResponse`, `/me` (obsolète) renvoie le même DTO que `/profile` via `ProfileService`. | `dto/BorrowResponse.java`, `AdminController`, `MeController`, `ProfileService` | Tests d'intégration, contrôle sur la stack |
| 7 | Journalisation des actions métier : emprunt, retour, création / modification / suppression de livre, réservation créée / annulée / supprimée, connexion réussie, appel de l'ancien `/authenticate`. | Contrôleurs | Logs de la stack |
| 8 | Pagination réelle côté Angular : listes des livres et des utilisateurs par pages de 10 (Précédent / Suivant). Les écrans qui recoupent toutes les données (emprunt, retour, réservations) chargent toujours la liste complète. | `books-list`, `users-list`, `DEFAULT_PAGE_SIZE` | Tests Karma |
| 9 | Plus de champ `password` dans le modèle `Users` ; un nouveau mot de passe se passe à part (`updateUser(id, user, newPassword?)`). | `_model/users.ts`, `users.service.ts` | Tests Karma |
| 15 | Test de bout en bout `KeycloakEndToEndIT` : Keycloak 24 réel (realm du projet) et PostgreSQL réel démarrés par Testcontainers ; 9 scénarios (jeton réel, `/auth/token`, 401 sans jeton ou jeton falsifié, 403, emprunt puis retour, Prometheus, `X-Request-ID`). Lancement : `./mvnw -Pe2e verify` (Docker requis), job CI `e2e`. | `e2e/KeycloakEndToEndIT.java`, profil Maven `e2e`, `.github/workflows/ci.yml` | 9/9 réussis |
| 16 | `OnPush` sur les 20 composants : `markForCheck()` après chaque réponse HTTP, le header se rafraîchit à chaque navigation, et le pipe `translate` marque la vue quand la langue change (`TranslationService.lang$`). | Tous les composants, `translate.pipe.ts` | Tests Karma, bascule de langue dans Chrome |
| 20 | Dernier `Optional.get()` remplacé par `orElseThrow()`. | `JwtService.java` | Tests |
| 23 | Injection par constructeur aussi dans la configuration de sécurité. | `WebSecurityConfiguration`, `JwtAuthenticationEntryPoint`, `LoggingAccessDeniedHandler` | Tests |
| 35 | Validation côté client ajoutée aux formulaires de connexion, d'ajout / modification de livre et de modification d'utilisateur (messages traduits, bouton désactivé tant que le formulaire est invalide). | Templates correspondants, `translations.ts` | Tests Karma |
| 37 | En-tête `Content-Security-Policy` (`script-src 'self'`, `connect-src` construit depuis `API_URL`), généré au démarrage du conteneur ; le CSS critique n'est plus injecté par un `onload` en ligne (`inlineCritical: false`). | `nginx.conf`, `csp.conf`, `40-env-config.sh`, `angular.json` | En-tête servi, aucune violation dans la console de Chrome |
| 41 | Tests unitaires (Mockito, sans Spring) pour tous les contrôleurs et pour `ProfileService`. | `BooksControllerTest`, `BorrowControllerTest`, `ReservationControllerTest`, `MeControllerTest`, `JwtControllerTest`, `AuthTokenControllerTest`, `ProfileServiceTest` | `mvn verify` |
| 42 | Seuils bloquants : JaCoCo 90 % des lignes / 80 % des branches (`mvn verify`), Karma 90 / 80 / 90 / 90 (`ng test --code-coverage`). Mesure : backend 96,0 % / 89,6 %, frontend 94,3 % instructions / 88,0 % branches. | `pom.xml`, `karma.conf.js` | `mvn verify`, `ng test --code-coverage` |
| 43 | Le seul type au nom français, `StatutReservation`, devient `ReservationStatus` (backend et frontend). Les valeurs de l'enum, les noms de tables et les champs JSON (`statut`, `livreId`…) sont inchangés : ni migration ni changement d'API. Les noms au pluriel (`Books`, `Users`) sont conservés pour la même raison. | `entity/ReservationStatus.java`, `_model/reservation.ts` | Tests |
| 66 | `caddy` (profile `https`) : `cap_drop: ALL` + `cap_add: NET_BIND_SERVICE`, `no-new-privileges`, limites 128 Mo / 0,25 CPU, healthcheck sur l'API d'administration. | `docker-compose.yml` | Démarré avec `--profile https` : conteneur sain, HTTPS 200 |
| 68 | Dépendance `micrometer-registry-prometheus` ajoutée : `/actuator/prometheus` répond (jeton requis). | `pom.xml` | 200 avec jeton, 401 sans ; test E2E |
| 69 | `logging.pattern.level=%5p [requestId=%X{requestId:-}]` : chaque ligne de log porte l'identifiant de la requête. | `application.properties` | Logs de la stack, test E2E |

---

*Dernière mise à jour : 14 septembre 2026*
