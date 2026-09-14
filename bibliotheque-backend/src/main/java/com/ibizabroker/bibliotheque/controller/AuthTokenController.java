package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.LoginRequest;
import com.ibizabroker.bibliotheque.dto.RefreshTokenRequest;
import com.ibizabroker.bibliotheque.dto.TokenResponse;
import com.ibizabroker.bibliotheque.service.KeycloakTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Connexion Keycloak visible dans Swagger : la réponse affiche l'access token
 * et le refresh token. Aucun jeton n'est requis pour appeler ces endpoints
 * (@SecurityRequirements vide : Swagger n'envoie pas d'en-tête Authorization,
 * qu'un jeton expiré ferait refuser en 401 avant même le contrôleur).
 */
@Tag(name = "Authentification")
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthTokenController {

    private final KeycloakTokenService keycloakTokenService;

    public AuthTokenController(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    @Operation(summary = "Se connecter : obtenir un access token et un refresh token",
            description = "Aucun jeton requis. 200 : copier accessToken dans « Authorize » > bearerAuth pour "
                    + "appeler l'API ; quand il expire (expiresIn secondes), envoyer refreshToken à "
                    + "POST /auth/refresh. 401 : identifiant ou mot de passe incorrect ; 503 : Keycloak indisponible.")
    @SecurityRequirements
    @PostMapping("/token")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return keycloakTokenService.login(request.getUsername(), request.getPassword());
    }

    @Operation(summary = "Renouveler les jetons avec le refresh token",
            description = "Aucun jeton requis. Renvoie un nouvel access token et un nouveau refresh token "
                    + "(l'ancien refresh token ne doit plus être réutilisé). 401 : refresh token invalide, "
                    + "expiré ou non émis par POST /auth/token ; 503 : Keycloak indisponible.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return keycloakTokenService.refresh(request.getRefreshToken());
    }
}
