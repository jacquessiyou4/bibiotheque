package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.TokenResponse;
import com.ibizabroker.bibliotheque.exceptions.ServiceUnavailableException;
import com.ibizabroker.bibliotheque.exceptions.UnauthorizedException;
import com.ibizabroker.bibliotheque.service.KeycloakTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de POST /auth/token et POST /auth/refresh : accessibles
 * sans jeton, réponse avec les deux jetons, erreurs 400 / 401 / 503, et
 * absence d'exigence de sécurité dans la documentation Swagger.
 * KeycloakTokenService et le décodeur JWT sont simulés.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "app.keycloak.jwks-uri=http://localhost:9999/realms/bibliotheque/protocol/openid-connect/certs",
        "app.keycloak.issuer-local=http://localhost:9999/realms/bibliotheque",
        "app.keycloak.issuer-internal=http://localhost:9999/realms/bibliotheque"
})
@AutoConfigureMockMvc
class AuthTokenControllerIntegrationTest {

    private static final TokenResponse JETONS =
            new TokenResponse("acces-123", "refresh-456", "Bearer", 1800, 1800, "profile email");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakTokenService keycloakTokenService;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void postToken_sansJeton_renvoieAccessTokenEtRefreshToken() throws Exception {
        when(keycloakTokenService.login("A1", "A1123")).thenReturn(JETONS);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"A1\",\"password\":\"A1123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acces-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-456"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.refreshExpiresIn").value(1800));
    }

    @Test
    void postToken_mauvaisMotDePasse_renvoie401AvecMessage() throws Exception {
        when(keycloakTokenService.login("A1", "faux"))
                .thenThrow(new UnauthorizedException("Identifiant ou mot de passe incorrect."));

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"A1\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiant ou mot de passe incorrect."));
    }

    @Test
    void postToken_champsManquants_renvoie400SansAppelerKeycloak() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(keycloakTokenService);
    }

    @Test
    void postToken_keycloakIndisponible_renvoie503() throws Exception {
        when(keycloakTokenService.login(anyString(), anyString()))
                .thenThrow(new ServiceUnavailableException("Service d'authentification indisponible, réessayez plus tard."));

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"A1\",\"password\":\"A1123\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void postRefresh_sansJeton_renvoieDeNouveauxJetons() throws Exception {
        when(keycloakTokenService.refresh("refresh-456")).thenReturn(JETONS);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acces-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-456"));
    }

    @Test
    void postRefresh_jetonExpire_renvoie401() throws Exception {
        when(keycloakTokenService.refresh("expire"))
                .thenThrow(new UnauthorizedException("Refresh token invalide ou expiré : reconnectez-vous avec POST /auth/token."));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expire\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postRefresh_sansRefreshToken_renvoie400() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.refreshToken").exists());
    }

    @Test
    void getToken_nEstPasOuvertSansJeton() throws Exception {
        // Seul POST est public : les autres méthodes restent protégées.
        mockMvc.perform(get("/auth/token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swagger_endpointsDeConnexion_nExigentAucunJeton() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/token'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/auth/refresh'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/auth/token'].post.tags[0]").value("Authentification"));
    }
}
