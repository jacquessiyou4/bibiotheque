package com.ibizabroker.bibliotheque.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de KeycloakJwtConfiguration : vérifie que le
 * jwtAuthenticationConverter traduit correctement les rôles Keycloak
 * (realm_access.roles) en autorités Spring Security (ROLE_X).
 */
class KeycloakJwtConfigurationTest {

    private KeycloakJwtConfiguration config;
    private org.springframework.core.convert.converter.Converter<Jwt, AbstractAuthenticationToken> converter;

    @BeforeEach
    void setUp() {
        config = new KeycloakJwtConfiguration();
        // Injecter les valeurs @Value via réflexion
        try {
            java.lang.reflect.Field jwksField = KeycloakJwtConfiguration.class.getDeclaredField("jwksUri");
            jwksField.setAccessible(true);
            jwksField.set(config, "http://localhost:9999/realms/bibliotheque/protocol/openid-connect/certs");

            java.lang.reflect.Field issuerLocalField = KeycloakJwtConfiguration.class.getDeclaredField("issuerLocal");
            issuerLocalField.setAccessible(true);
            issuerLocalField.set(config, "http://localhost:9999/realms/bibliotheque");

            java.lang.reflect.Field issuerInternalField = KeycloakJwtConfiguration.class.getDeclaredField("issuerInternal");
            issuerInternalField.setAccessible(true);
            // Valeur distincte de l'issuer local, pour vérifier que les DEUX sont acceptés.
            issuerInternalField.set(config, ISSUER_INTERNE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        converter = config.jwtAuthenticationConverter();
    }

    private static final String ISSUER_LOCAL = "http://localhost:9999/realms/bibliotheque";
    private static final String ISSUER_INTERNE = "http://keycloak:8080/realms/bibliotheque";

    // ------------------------------------------------------------------
    // Validation de l'issuer : seul le realm de l'application est accepté
    // ------------------------------------------------------------------
    @Test
    void issuerValidator_accepteLIssuerVuParLeNavigateur() {
        OAuth2TokenValidatorResult resultat = config.issuerValidator().validate(jwtAvecIssuer(ISSUER_LOCAL));

        assertThat(resultat.hasErrors()).isFalse();
    }

    @Test
    void issuerValidator_accepteLIssuerInterneDuReseauDocker() {
        OAuth2TokenValidatorResult resultat = config.issuerValidator().validate(jwtAvecIssuer(ISSUER_INTERNE));

        assertThat(resultat.hasErrors()).isFalse();
    }

    @Test
    void issuerValidator_refuseUnJetonDUnAutreRealmOuServeur() {
        OAuth2TokenValidatorResult resultat = config.issuerValidator()
                .validate(jwtAvecIssuer("http://pirate.example/realms/bibliotheque"));

        assertThat(resultat.hasErrors()).isTrue();
        OAuth2Error erreur = resultat.getErrors().iterator().next();
        assertThat(erreur.getErrorCode()).isEqualTo("invalid_iss");
        assertThat(erreur.getDescription()).contains("pirate.example");
    }

    @Test
    void issuerValidator_refuseUnJetonSansIssuer() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "A1");
        Jwt sansIssuer = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);

        OAuth2TokenValidatorResult resultat = config.issuerValidator().validate(sansIssuer);

        assertThat(resultat.hasErrors()).isTrue();
        assertThat(resultat.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_iss");
    }

    @Test
    void jwtDecoder_seConstruitSansContacterKeycloak() {
        // Les clés JWKS ne sont téléchargées qu'au premier décodage.
        JwtDecoder decoder = config.jwtDecoder();

        assertThat(decoder).isNotNull();
    }

    private Jwt jwtAvecIssuer(String issuer) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "A1");
        claims.put("iss", issuer);
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);
    }

    @Test
    void jwtAuthenticationConverter_traduitLesRolesKeycloakEnAuthoritiesSpring() {
        Jwt jwt = jwtWithRealmAccess("A1", "A1", new String[]{"ADHERENT", "BIBLIOTHECAIRE"});

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).hasSize(2);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADHERENT", "ROLE_BIBLIOTHECAIRE");
    }

    @Test
    void jwtAuthenticationConverter_utilisePreferredUsernameCommePrincipal() {
        Jwt jwt = jwtWithRealmAccess("preferred-user", "sub-user", new String[]{"User"});

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("preferred-user");
    }

    @Test
    void jwtAuthenticationConverter_fallbackSurSubjectSiPasPreferredUsername() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "subject-user");
        // Pas de "preferred_username"
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Collections.singletonList("User"));
        claims.put("realm_access", realmAccess);

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("subject-user");
    }

    @Test
    void jwtAuthenticationConverter_gereRealmAccessNull() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("preferred_username", "user1");
        // Pas de realm_access

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("user1");
    }

    @Test
    void jwtAuthenticationConverter_gereRolesVide() {
        Jwt jwt = jwtWithRealmAccess("user1", "user1", new String[]{});

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void jwtAuthenticationConverter_gereRealmAccessAvecRolesNonCollection() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("preferred_username", "user1");
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", "NOT_A_COLLECTION");
        claims.put("realm_access", realmAccess);

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    // ------------------------------------------------------------------

    private Jwt jwtWithRealmAccess(String preferredUsername, String subject, String[] roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("preferred_username", preferredUsername);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", java.util.Arrays.asList(roles));
        claims.put("realm_access", realmAccess);

        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);
    }
}
