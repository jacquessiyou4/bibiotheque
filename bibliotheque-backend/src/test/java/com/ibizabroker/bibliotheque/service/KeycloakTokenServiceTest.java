package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dto.TokenResponse;
import com.ibizabroker.bibliotheque.exceptions.ServiceUnavailableException;
import com.ibizabroker.bibliotheque.exceptions.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests unitaires de KeycloakTokenService : Keycloak est simulé par
 * MockRestServiceServer (aucun serveur), on vérifie le formulaire envoyé et
 * la traduction des réponses en jetons, 401 ou 503.
 */
class KeycloakTokenServiceTest {

    private static final String TOKEN_URI = "http://keycloak.test/realms/bibliotheque/protocol/openid-connect/token";
    private static final String CLIENT_ID = "bibliotheque-frontend";
    private static final String REPONSE_JETONS = "{\"access_token\":\"acces-123\",\"refresh_token\":\"refresh-456\","
            + "\"token_type\":\"Bearer\",\"expires_in\":1800,\"refresh_expires_in\":3600,"
            + "\"scope\":\"profile email\",\"session_state\":\"s1\"}";

    private MockRestServiceServer keycloak;
    private KeycloakTokenService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        service = new KeycloakTokenService(new RestTemplateBuilder(customizer), TOKEN_URI, CLIENT_ID);
        keycloak = customizer.getServer();
    }

    @AfterEach
    void verifierAppels() {
        keycloak.verify();
    }

    @Test
    void login_envoieLeGrantPasswordEtRenvoieLesDeuxJetons() {
        MultiValueMap<String, String> attendu = new LinkedMultiValueMap<>();
        attendu.add("grant_type", "password");
        attendu.add("client_id", CLIENT_ID);
        attendu.add("username", "A1");
        attendu.add("password", "mot de passe&=spécial");
        keycloak.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(attendu))
                .andRespond(withSuccess(REPONSE_JETONS, MediaType.APPLICATION_JSON));

        TokenResponse jetons = service.login("A1", "mot de passe&=spécial");

        assertThat(jetons.getAccessToken()).isEqualTo("acces-123");
        assertThat(jetons.getRefreshToken()).isEqualTo("refresh-456");
        assertThat(jetons.getTokenType()).isEqualTo("Bearer");
        assertThat(jetons.getExpiresIn()).isEqualTo(1800);
        assertThat(jetons.getRefreshExpiresIn()).isEqualTo(3600);
        assertThat(jetons.getScope()).isEqualTo("profile email");
    }

    @Test
    void login_identifiantsRefusesParKeycloak_leve401() {
        keycloak.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}"));

        assertThatThrownBy(() -> service.login("A1", "faux"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Identifiant ou mot de passe incorrect.");
    }

    @Test
    void refresh_envoieLeGrantRefreshTokenEtRenvoieDeNouveauxJetons() {
        MultiValueMap<String, String> attendu = new LinkedMultiValueMap<>();
        attendu.add("grant_type", "refresh_token");
        attendu.add("client_id", CLIENT_ID);
        attendu.add("refresh_token", "refresh-456");
        keycloak.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(attendu))
                .andRespond(withSuccess(REPONSE_JETONS, MediaType.APPLICATION_JSON));

        TokenResponse jetons = service.refresh("refresh-456");

        assertThat(jetons.getAccessToken()).isEqualTo("acces-123");
        assertThat(jetons.getRefreshToken()).isEqualTo("refresh-456");
    }

    @Test
    void refresh_jetonExpireOuInvalide_leve401() {
        // Keycloak répond 400 invalid_grant pour un refresh token expiré.
        keycloak.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"error_description\":\"Token is not active\"}"));

        assertThatThrownBy(() -> service.refresh("expire"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("POST /auth/token");
    }

    @Test
    void keycloakEnErreurServeur_leve503() {
        keycloak.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> service.login("A1", "A1123"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void keycloakInjoignable_leve503() {
        keycloak.expect(requestTo(TOKEN_URI)).andRespond(request -> {
            throw new ResourceAccessException("Connection refused");
        });

        assertThatThrownBy(() -> service.login("A1", "A1123"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void reponseSansAccessToken_leve503() {
        keycloak.expect(requestTo(TOKEN_URI)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.login("A1", "A1123"))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
