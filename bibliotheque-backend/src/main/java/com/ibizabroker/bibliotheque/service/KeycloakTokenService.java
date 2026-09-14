package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dto.TokenResponse;
import com.ibizabroker.bibliotheque.exceptions.ServiceUnavailableException;
import com.ibizabroker.bibliotheque.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Obtient les jetons Keycloak côté serveur, pour les afficher dans Swagger
 * (POST /auth/token).
 *
 * L'appel part du backend : pas de CORS, et depuis Docker l'endpoint token
 * est joint par le réseau interne (keycloak:8080). Les jetons portent alors
 * l'issuer interne, accepté par KeycloakJwtConfiguration.
 * Le mot de passe n'est jamais journalisé.
 */
@Slf4j
@Service
public class KeycloakTokenService {

    private static final ParameterizedTypeReference<Map<String, Object>> REPONSE_JSON =
            new ParameterizedTypeReference<Map<String, Object>>() {};
    private static final String INDISPONIBLE = "Service d'authentification indisponible, réessayez plus tard.";

    private final RestTemplate restTemplate;
    private final String tokenUri;
    private final String clientId;

    public KeycloakTokenService(RestTemplateBuilder restTemplateBuilder,
                                @Value("${app.keycloak.token-uri}") String tokenUri,
                                @Value("${app.keycloak.client-id}") String clientId) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
    }

    public TokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", username);
        form.add("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> corps;
        try {
            corps = restTemplate.exchange(tokenUri, HttpMethod.POST, new HttpEntity<>(form, headers), REPONSE_JSON)
                    .getBody();
        } catch (HttpClientErrorException ex) {
            // 400 invalid_grant (mauvais identifiants, compte désactivé) ou 401 : refus du client.
            log.warn("[SECURITE] Connexion Keycloak refusée - statut={}", ex.getRawStatusCode());
            throw new UnauthorizedException("Identifiant ou mot de passe incorrect.");
        } catch (RestClientException ex) {
            log.error("Keycloak injoignable ou en erreur ({})", tokenUri, ex);
            throw new ServiceUnavailableException(INDISPONIBLE);
        }

        if (corps == null || corps.get("access_token") == null) {
            log.error("Réponse Keycloak sans access_token ({})", tokenUri);
            throw new ServiceUnavailableException(INDISPONIBLE);
        }
        return new TokenResponse(
                texte(corps.get("access_token")),
                texte(corps.get("refresh_token")),
                texte(corps.get("token_type")),
                nombre(corps.get("expires_in")),
                nombre(corps.get("refresh_expires_in")),
                texte(corps.get("scope")));
    }

    private static String texte(Object valeur) {
        return valeur == null ? null : valeur.toString();
    }

    private static long nombre(Object valeur) {
        return valeur instanceof Number ? ((Number) valeur).longValue() : 0L;
    }
}
