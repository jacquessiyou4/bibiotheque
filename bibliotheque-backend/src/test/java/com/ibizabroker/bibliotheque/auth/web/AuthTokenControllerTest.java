package com.ibizabroker.bibliotheque.auth.web;

import com.ibizabroker.bibliotheque.shared.error.UnauthorizedException;
import com.ibizabroker.bibliotheque.auth.internal.KeycloakTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires d'AuthTokenController : KeycloakTokenService simulé, aucun
 * contexte Spring. Accès sans jeton et erreurs HTTP : voir
 * AuthTokenControllerIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthTokenControllerTest {

    @Mock
    private KeycloakTokenService keycloakTokenService;

    @InjectMocks
    private AuthTokenController controller;

    @Test
    void login_renvoieLesJetonsObtenusAupresDeKeycloak() {
        TokenResponse jetons = new TokenResponse("acces", "refresh", "Bearer", 1800, 1800, "profile email");
        when(keycloakTokenService.login("A1", "A1123")).thenReturn(jetons);

        assertThat(controller.login(new LoginRequest("A1", "A1123"))).isSameAs(jetons);
    }

    @Test
    void login_identifiantsRefuses_laissePasserLe401() {
        when(keycloakTokenService.login("A1", "faux"))
                .thenThrow(new UnauthorizedException("Identifiant ou mot de passe incorrect."));

        assertThatThrownBy(() -> controller.login(new LoginRequest("A1", "faux")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
