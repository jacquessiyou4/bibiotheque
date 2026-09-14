package com.ibizabroker.bibliotheque.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI et connexion depuis Swagger UI.
 *
 * Deux façons de s'authentifier avec le bouton « Authorize » :
 * - keycloak : saisir identifiant / mot de passe (flow OAuth2 « password ») ;
 *   Swagger UI demande lui-même le jeton à Keycloak et l'ajoute aux requêtes.
 * - bearerAuth : coller un jeton Keycloak déjà obtenu.
 * Les deux exigences sont déclarées séparément : l'une OU l'autre suffit.
 */
@Configuration
public class OpenApiConfiguration {

    static final String BEARER_AUTH = "bearerAuth";
    static final String KEYCLOAK_AUTH = "keycloak";

    /**
     * Issuer vu par le NAVIGATEUR : c'est Swagger UI (dans le navigateur) qui
     * appelle l'endpoint token, il faut donc l'adresse publiée de Keycloak.
     */
    private final String issuerLocal;

    public OpenApiConfiguration(@Value("${app.keycloak.issuer-local}") String issuerLocal) {
        this.issuerLocal = issuerLocal;
    }

    @Bean
    public OpenAPI bibliothequeOpenApi() {
        OAuthFlow passwordFlow = new OAuthFlow()
                .tokenUrl(issuerLocal + "/protocol/openid-connect/token")
                .scopes(new Scopes());

        return new OpenAPI()
                .info(new Info()
                        .title("bibliothèque")
                        .description("API de gestion de bibliothèque. Pour tester : bouton « Authorize », "
                                + "section « keycloak », saisir un compte (ex. admin / admin123 ou A1 / A1123), "
                                + "laisser client_id = bibliotheque-frontend, sans client_secret. "
                                + "Pour voir l'access token et le refresh token : POST /auth/token, "
                                + "puis POST /auth/refresh quand l'access token expire."))
                .components(new Components()
                        .addSecuritySchemes(KEYCLOAK_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Connexion Keycloak (realm bibliotheque)")
                                .flows(new OAuthFlows().password(passwordFlow)))
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton d'accès Keycloak déjà obtenu (sans le préfixe Bearer)")))
                .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
