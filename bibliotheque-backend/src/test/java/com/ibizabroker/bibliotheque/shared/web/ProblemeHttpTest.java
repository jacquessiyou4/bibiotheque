package com.ibizabroker.bibliotheque.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemeHttpTest {

    @Test
    void ecrire_produitUnCorpsProblemJsonAvecInstance() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ProblemeHttp.ecrire(response, new ObjectMapper(), HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED",
                "Votre session a expiré.", "/profile");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"type\":\"https://bibliotheque.local/problemes/session-expired\"")
                .contains("\"title\":\"Unauthorized\"")
                .contains("\"code\":\"SESSION_EXPIRED\"")
                .contains("\"instance\":\"/profile\"");
    }

    @Test
    void profilProd_masqueLesDetailsInternesEtCoupeSwagger() throws Exception {
        Properties prod = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.properties")) {
            assertThat(in).isNotNull();
            prod.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        assertThat(prod)
                .containsEntry("server.error.include-message", "never")
                .containsEntry("server.error.include-stacktrace", "never")
                .containsEntry("management.endpoint.health.show-details", "never")
                .containsEntry("springdoc.api-docs.enabled", "false")
                .containsEntry("springdoc.swagger-ui.enabled", "false");
    }
}
