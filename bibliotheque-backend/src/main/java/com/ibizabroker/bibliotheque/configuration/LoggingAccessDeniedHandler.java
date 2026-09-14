package com.ibizabroker.bibliotheque.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire 403 pour les refus au niveau du filtre de sécurité :
 * journalise la tentative d'accès refusée (utilisateur, autorités, URI,
 * motif) et renvoie un corps JSON uniforme.
 */
@Component
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public LoggingAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String utilisateur = auth != null ? auth.getName() : "anonyme";
        String autorites = auth != null ? auth.getAuthorities().toString() : "[]";

        log.warn("[SECURITE] Accès refusé [403] - URI: {} - Utilisateur: {} - Autorités: {} - Motif: {}",
                request.getRequestURI(), utilisateur, autorites,
                accessDeniedException != null ? accessDeniedException.getMessage() : "accès interdit");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", accessDeniedException != null ? accessDeniedException.getMessage() : "Accès interdit.");
        body.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
