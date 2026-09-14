package com.ibizabroker.bibliotheque.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Point d'entrée 401 : appelé quand une requête non authentifiée atteint une
 * ressource protégée. Distingue trois situations pour renvoyer un message
 * explicite (bonus « expérience du jeton expiré ») :
 *   - aucun jeton fourni        -> "Authentification requise"
 *   - jeton présent mais expiré -> "Votre session a expiré"
 *   - jeton présent, autre      -> "Jeton invalide"
 * Journalise systématiquement la tentative d'accès refusée (401).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String uri = request.getRequestURI();
        boolean jetonPresent = aUnJeton(request);

        String raison;
        String message;
        if (!jetonPresent) {
            raison = "aucun jeton fourni";
            message = "Authentification requise : aucun jeton valide fourni.";
        } else if (estExpire(authException)) {
            raison = "jeton expiré";
            message = "Votre session a expiré. Veuillez vous reconnecter.";
        } else {
            raison = "jeton invalide";
            message = "Jeton invalide ou expiré. Veuillez vous reconnecter.";
        }

        String detail = authException != null ? authException.getMessage() : "authentification non aboutie";
        log.warn("[SECURITE] Accès refusé [401] - URI: {} - Raison: {} - Détail: {}", uri, raison, detail);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", message);
        body.put("path", uri);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private boolean aUnJeton(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        return bearer != null && bearer.startsWith("Bearer ") && bearer.length() > 7;
    }

    private boolean estExpire(AuthenticationException authException) {
        if (authException == null) {
            return false;
        }
        String m = authException.getMessage();
        return m != null && (m.toLowerCase().contains("expir") || m.toLowerCase().contains("expired"));
    }
}
