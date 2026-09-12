# Best Practices — Bibliothèque

> Recommandations concrètes pour améliorer la sécurité, l'architecture et la
> qualité du code de l'application de gestion de bibliothèque.
>
> Classées par priorité : 🔴 critique → 🟠 haute → 🟡 moyenne → 🟢 nice-to-have.

---

## 🔴 Priorité critique — Sécurité & fiabilité

### 1. Supprimer tous les `console.log` du code frontend

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

### 6. Introduire des DTOs pour l'API (Backend)

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

### 7. Remplacer `console.log` par un logging structuré (Backend)

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

### 8. Ajouter la pagination

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

### 9. Nettoyer les modèles frontend

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

### 12. Uniformiser la langue des tests

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

### 14. Mettre en place un pipeline CI/CD

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

### 15. Ajouter un test E2E avec Keycloak

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

### 16. Activer `OnPush` change detection

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

## Résumé — Plan d'action

| Priorité | Action | Effort | Impact |
|---|---|---|---|
| 🔴 | Supprimer `console.log` frontend | Faible | Sécurité |
| 🔴 | Ajouter HTTPS (reverse proxy) | Moyen | Sécurité |
| 🟠 | ErrorHandler global Angular | Moyen | UX / Maintenance |
| 🟠 | DTOs pour l'API backend | Moyen | Sécurité / API |
| 🟠 | Pagination | Moyen | Performance |
| 🟡 | Validation des entrées (`@Valid`) | Faible | Robustesse |
| 🟡 | `switchMap` au lieu de `subscribe` imbriqués | Faible | Lisibilité |
| 🟢 | Pipeline CI/CD | Moyen | Fiabilité |
| 🟢 | Swagger annotations | Faible | Documentation |

---

*Dernière mise à jour : septembre 2026*
