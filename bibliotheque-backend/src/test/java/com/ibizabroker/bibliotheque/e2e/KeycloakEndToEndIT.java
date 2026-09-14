package com.ibizabroker.bibliotheque.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de bout en bout avec un VRAI Keycloak (realm du projet importé) et une
 * vraie base PostgreSQL migrée par Flyway : aucun décodeur JWT simulé, les
 * jetons sont signés par Keycloak et validés par le backend via JWKS.
 *
 * Lancement : ./mvnw -Pe2e verify (Docker requis). Comptes du realm :
 * admin / admin123 (Admin, BIBLIOTHECAIRE), A1 / A1123 (User, ADHERENT).
 */
@Testcontainers
// Depuis Spring Boot 2.4, @SpringBootTest désactive l'export des métriques
// (/actuator/prometheus absent) : on le réactive pour tester l'application réelle.
@org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakEndToEndIT {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJET =
            new ParameterizedTypeReference<Map<String, Object>>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_LISTE =
            new ParameterizedTypeReference<List<Map<String, Object>>>() {};

    /**
     * Sous-classes typées : sur un GenericContainer&lt;?&gt;, les méthodes « with* »
     * chaînées renvoient un type capturé et javac refuse les varargs.
     */
    static class Postgres extends PostgreSQLContainer<Postgres> {
        Postgres() {
            super(DockerImageName.parse("postgres:16-alpine"));
        }
    }

    static class Keycloak extends GenericContainer<Keycloak> {
        Keycloak() {
            super(DockerImageName.parse("quay.io/keycloak/keycloak:24.0.5"));
        }
    }

    @Container
    static final Postgres POSTGRES = new Postgres().withDatabaseName("bibliotheque");

    @Container
    static final Keycloak KEYCLOAK = new Keycloak()
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forHostPath("../keycloak/realm-bibliotheque.json"),
                    "/opt/keycloak/data/import/realm-bibliotheque.json")
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/realms/bibliotheque").forPort(8080).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(4)));

    @DynamicPropertySource
    static void configurer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Le test et le backend joignent Keycloak par la même adresse : l'issuer
        // des jetons est donc celui qu'on déclare au backend.
        registry.add("app.keycloak.issuer-local", KeycloakEndToEndIT::realm);
        registry.add("app.keycloak.issuer-internal", KeycloakEndToEndIT::realm);
        registry.add("app.keycloak.jwks-uri", () -> realm() + "/protocol/openid-connect/certs");
        registry.add("app.keycloak.token-uri", () -> realm() + "/protocol/openid-connect/token");
        registry.add("jwt.secret", () -> "secret-des-tests-de-bout-en-bout-uniquement-0123456789");
    }

    private static String realm() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/bibliotheque";
    }

    @Autowired
    private TestRestTemplate api;

    @org.junit.jupiter.api.BeforeEach
    void bufferiserLesRequetes() {
        // HttpURLConnection lève HttpRetryException quand un POST envoyé en
        // streaming reçoit un 401 : on bufferise le corps pour lire la réponse.
        org.springframework.http.client.SimpleClientHttpRequestFactory fabrique =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        fabrique.setOutputStreaming(false);
        api.getRestTemplate().setRequestFactory(fabrique);
    }

    // ------------------------------------------------------------------
    // Jetons Keycloak réels
    // ------------------------------------------------------------------

    @Test
    void jetonKeycloakReel_donneAccesAuProfilDeLUtilisateur() {
        ResponseEntity<Map<String, Object>> reponse = appel(HttpMethod.GET, "/profile", jetonKeycloak("A1", "A1123"), null);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).containsEntry("username", "a1");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) reponse.getBody().get("roles");
        assertThat(roles).contains("ADHERENT");
    }

    @Test
    void postAuthToken_obtientDesJetonsReelsAcceptesParLApi() {
        Map<String, String> identifiants = new HashMap<>();
        identifiants.put("username", "admin");
        identifiants.put("password", "admin123");

        ResponseEntity<Map<String, Object>> jetons = appel(HttpMethod.POST, "/auth/token", null, identifiants);

        assertThat(jetons.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) jetons.getBody().get("refreshToken")).isNotBlank();
        String accessToken = (String) jetons.getBody().get("accessToken");
        ResponseEntity<Map<String, Object>> utilisateurs = appel(HttpMethod.GET, "/admin/users?size=5", accessToken, null);
        assertThat(utilisateurs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) utilisateurs.getBody().get("content")).isNotEmpty();
    }

    @Test
    void postAuthToken_mauvaisMotDePasse_renvoie401() {
        Map<String, String> identifiants = new HashMap<>();
        identifiants.put("username", "A1");
        identifiants.put("password", "mauvais");

        ResponseEntity<Map<String, Object>> reponse = appel(HttpMethod.POST, "/auth/token", null, identifiants);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reponse.getBody()).containsEntry("message", "Identifiant ou mot de passe incorrect.");
    }

    // ------------------------------------------------------------------
    // Refus 401 / 403
    // ------------------------------------------------------------------

    @Test
    void sansJeton_renvoie401() {
        assertThat(appel(HttpMethod.GET, "/api/reservations", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void jetonFalsifie_renvoie401() {
        String jeton = jetonKeycloak("A1", "A1123");
        int milieuSignature = jeton.lastIndexOf('.') + (jeton.length() - jeton.lastIndexOf('.')) / 2;
        char remplacement = jeton.charAt(milieuSignature) == 'A' ? 'B' : 'A';
        String falsifie = jeton.substring(0, milieuSignature) + remplacement + jeton.substring(milieuSignature + 1);

        assertThat(appel(HttpMethod.GET, "/profile", falsifie, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adherent_actionReserveeAuBibliothecaire_renvoie403() {
        ResponseEntity<Map<String, Object>> reponse =
                appel(HttpMethod.DELETE, "/api/reservations/1", jetonKeycloak("A1", "A1123"), null);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // Parcours métier complet
    // ------------------------------------------------------------------

    @Test
    void adherent_empruntePuisRendUnLivre() {
        String jeton = jetonKeycloak("A1", "A1123");
        Number userId = (Number) appel(HttpMethod.GET, "/profile", jeton, null).getBody().get("userId");

        Map<String, Object> emprunt = new HashMap<>();
        emprunt.put("bookId", 1);
        emprunt.put("userId", userId);
        ResponseEntity<String> empruntReponse = api.exchange("/borrow", HttpMethod.POST,
                new HttpEntity<>(emprunt, entetes(jeton)), String.class);
        assertThat(empruntReponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<List<Map<String, Object>>> emprunts = api.exchange("/borrow/user/" + userId, HttpMethod.GET,
                new HttpEntity<>(entetes(jeton)), JSON_LISTE);
        Map<String, Object> enCours = emprunts.getBody().stream()
                .filter(e -> ((Number) e.get("bookId")).intValue() == 1 && e.get("returnDate") == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Emprunt du livre 1 introuvable : " + emprunts.getBody()));

        Map<String, Object> retour = new HashMap<>();
        retour.put("borrowId", enCours.get("borrowId"));
        retour.put("bookId", 1);
        retour.put("userId", userId);
        ResponseEntity<Map<String, Object>> rendu = appel(HttpMethod.PUT, "/borrow", jeton, retour);

        assertThat(rendu.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rendu.getBody().get("returnDate")).isNotNull();
    }

    // ------------------------------------------------------------------
    // Observabilité
    // ------------------------------------------------------------------

    @Test
    void prometheus_exposeLesMetriquesAvecUnJetonAdmin() {
        String jeton = jetonKeycloak("admin", "admin123");

        ResponseEntity<String> metriques = api.exchange("/actuator/prometheus", HttpMethod.GET,
                new HttpEntity<>(entetes(jeton)), String.class);

        assertThat(metriques.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metriques.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    void requestId_estRenvoyeTelQuelPourCorrelerLesLogs() {
        HttpHeaders entetes = entetes(jetonKeycloak("A1", "A1123"));
        entetes.set("X-Request-ID", "e2e-correlation-42");

        ResponseEntity<String> reponse = api.exchange("/profile", HttpMethod.GET, new HttpEntity<>(entetes), String.class);

        assertThat(reponse.getHeaders().getFirst("X-Request-ID")).isEqualTo("e2e-correlation-42");
    }

    // ------------------------------------------------------------------

    /** Jeton obtenu directement auprès de Keycloak (flow password, client public du realm). */
    private String jetonKeycloak(String username, String password) {
        MultiValueMap<String, String> formulaire = new LinkedMultiValueMap<>();
        formulaire.add("grant_type", "password");
        formulaire.add("client_id", "bibliotheque-frontend");
        formulaire.add("username", username);
        formulaire.add("password", password);
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map<String, Object>> reponse = new RestTemplate().exchange(
                realm() + "/protocol/openid-connect/token", HttpMethod.POST,
                new HttpEntity<>(formulaire, entetes), JSON_OBJET);
        return (String) reponse.getBody().get("access_token");
    }

    private ResponseEntity<Map<String, Object>> appel(HttpMethod methode, String chemin, String jeton, Object corps) {
        return api.exchange(chemin, methode, new HttpEntity<>(corps, entetes(jeton)), JSON_OBJET);
    }

    private HttpHeaders entetes(String jeton) {
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_JSON);
        if (jeton != null) {
            entetes.setBearerAuth(jeton);
        }
        return entetes;
    }
}
