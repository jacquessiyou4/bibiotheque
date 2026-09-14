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
 * Obtient et renouvelle les jetons Keycloak côté serveur, pour les afficher
 * dans Swagger (POST /auth/token, POST /auth/refresh).
 *
 * L'appel part du backend : pas de CORS, et depuis Docker l'endpoint token
 * est joint par le réseau interne (keycloak:8080). Les jetons portent alors
 * l'issuer interne, accepté par KeycloakJwtConfiguration ; un refresh token
 * doit être renouvelé par le même chemin que celui qui l'a émis.
 * Le mot de passe n'est jamais journalisé.
 */
@Slf4j
@Service
public class KeycloakTokenService {

    private static final ParameterizedTypeReference<Map<String, Object>> REPONSE_JSON =
            new ParameterizedTypeReference<Map<String, Object>>() {};

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
        return demanderJetons(form, "Identifiant ou mot de passe incorrect.");
    }

    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);
        return demanderJetons(form,
                "Refresh token invalide ou expiré : reconnectez-vous avec POST /auth/token.");
    }

    private TokenResponse demanderJetons(MultiValueMap<String, String> form, String messageRefus) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> corps;
        try {
            corps = restTemplate.exchange(tokenUri, HttpMethod.POST, new HttpEntity<>(form, headers), REPONSE_JSON)
                    .getBody();
        } catch (HttpClientErrorException ex) {
            // 400 invalid_grant (mauvais identifiants, compte désactivé, refresh
            // token expiré ou d'un autre issuer) ou 401 : refus du client.
            log.warn("[SECURITE] Jetons Keycloak refusés - grant_type={} - statut={}",
                    form.getFirst("grant_type"), ex.getRawStatusCode());
            throw new UnauthorizedException(messageRefus);
        } catch (RestClientException ex) {
            log.error("Keycloak injoignable ou en erreur ({})", tokenUri, ex);
            throw new ServiceUnavailableException("Service d'authentification indisponible, réessayez plus tard.");
        }

        if (corps == null || corps.get("access_token") == null) {
            log.error("Réponse Keycloak sans access_token ({})", tokenUri);
            throw new ServiceUnavailableException("Service d'authentification indisponible, réessayez plus tard.");
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
