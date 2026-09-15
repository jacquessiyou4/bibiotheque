package com.ibizabroker.bibliotheque.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Filtre d'audit de sécurité qui :
 * 1. Extrait ou génère un X-Request-ID pour corréler les logs
 * 2. Journalise les tentatives d'accès refusées (401/403)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityAuditFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    // Lu par ProblemeHttp : chaque réponse d'erreur rappelle l'identifiant de la requête.
    private static final String MDC_REQUEST_ID = ProblemeHttp.MDC_REQUEST_ID;
    /**
     * Utilisateur à l'origine d'un refus, posé par GlobalExceptionHandler. Ce
     * filtre entoure la chaîne Spring Security : quand il journalise (après la
     * chaîne), le contexte de sécurité est déjà vidé et tout le monde
     * apparaîtrait « anonyme ».
     */
    public static final String ATTRIBUT_UTILISATEUR = "securite.utilisateur";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Extraire ou générer un request ID unique
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_REQUEST_ID, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            int status = httpResponse.getStatus();
            if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_FORBIDDEN) {
                log.warn("[SECURITE-AUDIT] Tentative d'accès refusée - statut={} - méthode={} - URI={} - utilisateur={}",
                        status, httpRequest.getMethod(), httpRequest.getRequestURI(), utilisateurCourant(httpRequest));
            }
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String utilisateurCourant(HttpServletRequest request) {
        Object utilisateur = request.getAttribute(ATTRIBUT_UTILISATEUR);
        if (utilisateur != null) {
            return utilisateur.toString();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() != null
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return auth.getName();
        }
        return "anonyme";
    }
}
