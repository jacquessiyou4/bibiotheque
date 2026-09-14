package com.ibizabroker.bibliotheque.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du point d'entrée 401. Vérifie la distinction entre
 * « aucun jeton fourni », « jeton expiré » et « jeton invalide » (règle
 * 401 du module Réservation), sans conteneur ni Spring context.
 */
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint(new ObjectMapper());
        request = new MockHttpServletRequest("GET", "/api/reservations");
        response = new MockHttpServletResponse();
    }

    @Test
    void commence_sansJeton_renvoie401AvecMessageAuthentificationRequise() throws Exception {
        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"message\":\"Authentification requise : aucun jeton valide fourni.\"")
                .contains("\"path\":\"/api/reservations\"")
                .contains("\"error\":\"Unauthorized\"");
    }

    @Test
    void commence_avecJetonExpiré_renvoieLeMessageDeSessionExpirée() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jeton-expire");
        AuthenticationException exception = new BadCredentialsException(
                "Jwt expired at 2026-09-01T00:00:00Z, current time is 2026-09-11T00:00:00Z");

        entryPoint.commence(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"message\":\"Votre session a expiré. Veuillez vous reconnecter.\"");
    }

    @Test
    void commence_avecJetonInvalide_renvoieLeMessageDeJetonInvalide() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jeton-falsifié");

        entryPoint.commence(request, response, new BadCredentialsException("signature non valide"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"message\":\"Jeton invalide ou expiré. Veuillez vous reconnecter.\"");
    }

    @Test
    void commence_avecUnEnTêteQuiNestPasBearer_estTraitéCommeSansJeton() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("Authentification requise");
    }

    @Test
    void commence_avecBearerVide_estTraitéCommeSansJeton() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");

        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("Authentification requise");
    }

    @Test
    void commence_renvoieUnContenuJson() throws Exception {
        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    }
}
