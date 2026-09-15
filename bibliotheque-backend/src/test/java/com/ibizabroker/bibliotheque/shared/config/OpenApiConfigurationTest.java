package com.ibizabroker.bibliotheque.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de la documentation OpenAPI : Swagger UI doit permettre de
 * se connecter à Keycloak (flow password) ou de coller un jeton, sans serveur.
 */
class OpenApiConfigurationTest {

    private static final String ISSUER = "http://localhost:8081/realms/bibliotheque";

    private final OpenAPI openApi = new OpenApiConfiguration(ISSUER).bibliothequeOpenApi();

    @Test
    void keycloak_flowPassword_pointeSurLEndpointTokenDuRealmVuDuNavigateur() {
        SecurityScheme keycloak = openApi.getComponents().getSecuritySchemes()
                .get(OpenApiConfiguration.KEYCLOAK_AUTH);

        assertThat(keycloak.getType()).isEqualTo(SecurityScheme.Type.OAUTH2);
        OAuthFlow password = keycloak.getFlows().getPassword();
        assertThat(password.getTokenUrl()).isEqualTo(ISSUER + "/protocol/openid-connect/token");
        assertThat(keycloak.getFlows().getAuthorizationCode()).isNull();
    }

    @Test
    void bearerAuth_resteDisponiblePourCollerUnJeton() {
        SecurityScheme bearer = openApi.getComponents().getSecuritySchemes()
                .get(OpenApiConfiguration.BEARER_AUTH);

        assertThat(bearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearer.getScheme()).isEqualTo("bearer");
        assertThat(bearer.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void exigencesDeSecurite_lUneOuLAutreSuffit() {
        // Deux SecurityRequirement distincts = OU en OpenAPI ; un seul objet
        // contenant les deux schémas imposerait les deux à la fois.
        List<SecurityRequirement> exigences = openApi.getSecurity();

        assertThat(exigences).hasSize(2);
        assertThat(exigences.get(0)).containsOnlyKeys(OpenApiConfiguration.KEYCLOAK_AUTH);
        assertThat(exigences.get(1)).containsOnlyKeys(OpenApiConfiguration.BEARER_AUTH);
    }
}
