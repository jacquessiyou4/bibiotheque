package com.ibizabroker.bibliotheque.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Intégration Keycloak côté backend : le backend joue le rôle d'un
 * <em>resource server</em> OAuth2. Il valide les jetons d'accès signés
 * par Keycloak (récupération des clés publiques via l'endpoint JWKS) et
 * traduit les rôles Keycloak ("realm_access.roles") en autorités Spring
 * Security (ROLE_Admin, ROLE_User), alignées sur les @PreAuthorize et sur
 * les rôles de l'application existante.
 */
@Configuration
public class KeycloakJwtConfiguration {

    @Value("${app.keycloak.jwks-uri}")
    private String jwksUri;

    /**
     * Issuer vu par le NAVIGATEUR (jetons obtenus via localhost:8081).
     */
    @Value("${app.keycloak.issuer-local}")
    private String issuerLocal;

    /**
     * Issuer vu en interne (service à service via keycloak:8080).
     */
    @Value("${app.keycloak.issuer-internal}")
    private String issuerInternal;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();

        // Validations par défaut (dates iat/exp) + acceptation des deux issuer.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), issuerValidator()));
        return decoder;
    }

    /**
     * N'accepte que les jetons émis par le realm Keycloak de l'application,
     * vu du navigateur (issuer local) ou du réseau Docker (issuer interne).
     * Méthode dédiée (et non lambda inline) pour être testée sans serveur
     * Keycloak ni clés JWKS.
     */
    OAuth2TokenValidator<Jwt> issuerValidator() {
        return token -> {
            // getIssuer() renvoie un URL selon la version de Spring Security :
            // on lit le claim brut « iss » pour comparer en tant que String.
            String issuer = token.getClaimAsString("iss");
            if (issuerLocal.equals(issuer) || issuerInternal.equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_iss", "Issuer inconnu : " + issuer, null));
        };
    }

    /**
     * Convertit un JWT Keycloak en authentication Spring Security.
     * Les rôles du realm (« realm_access.roles ») deviennent des autorités
     * « ROLE_<nomDuRole> » (casse préservée : ROLE_Admin, ROLE_User).
     * Le principal est le nom d'utilisateur Keycloak (preferred_username).
     * Implémenté en classe anonyme (et non en lambda) pour que Spring
     * résolve les types génériques du Converter lors de l'enregistrement
     * dans la ConversionService MVC.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null) {
                    Object roles = realmAccess.get("roles");
                    if (roles instanceof Collection) {
                        for (Object role : (Collection<?>) roles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
                        }
                    }
                }
                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getSubject();
                }
                return new JwtAuthenticationToken(jwt, authorities, username);
            }
        };
    }
}