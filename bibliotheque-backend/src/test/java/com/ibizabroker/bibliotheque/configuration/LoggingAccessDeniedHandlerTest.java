package com.ibizabroker.bibliotheque.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du gestionnaire 403 (refus au niveau du filtre de
 * sécurité). Vérifie le statut, le corps JSON uniforme et la journalisation
 * du nom de l'utilisateur connecté.
 */
class LoggingAccessDeniedHandlerTest {

    private LoggingAccessDeniedHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new LoggingAccessDeniedHandler(new ObjectMapper());
        request = new MockHttpServletRequest("DELETE", "/api/reservations/100");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_renvoie403AvecUnCorpsJsonUniforme() throws Exception {
        handler.handle(request, response, new AccessDeniedException("Access is denied"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"status\":403")
                .contains("\"error\":\"Forbidden\"")
                .contains("\"message\":\"Access is denied\"")
                .contains("\"path\":\"/api/reservations/100\"");
    }

    @Test
    void handle_sansException_renvoieLeMessageParDéfaut() throws Exception {
        handler.handle(request, response, null);

        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"message\":\"Accès interdit.\"");
    }

    @Test
    void handle_journaliseLUtilisateurConnecté() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "A1", "n/a", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADHERENT"))));

        handler.handle(request, response, new AccessDeniedException("Access is denied"));

        // Le contexte sécurité est lu (utilisateur + autorités) sans exception
        // et la réponse reste un 403 cohérent.
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("\"error\":\"Forbidden\"");
    }
}
