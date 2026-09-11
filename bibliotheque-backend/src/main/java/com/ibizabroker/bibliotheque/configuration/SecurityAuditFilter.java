package com.ibizabroker.bibliotheque.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Filtre d'audit de sécurité (bonus « journaliser les tentatives d'accès
 * refusées »). Placé au tout début de la chaîne, il observe le code de statut
 * final de chaque requête : toute réponse 401 ou 403 est journalisée avec la
 * méthode, l'URI et l'utilisateur courant. Complète ainsi les gestionnaires
 * 401/403 quel que soit le niveau (filtre ou @PreAuthorize au niveau méthode).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityAuditFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        chain.doFilter(request, response);

        int status = httpResponse.getStatus();
        if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_FORBIDDEN) {
            log.warn("[SECURITE-AUDIT] Tentative d'accès refusée - statut={} - méthode={} - URI={} - utilisateur={}",
                    status, httpRequest.getMethod(), httpRequest.getRequestURI(), utilisateurCourant());
        }
    }

    private String utilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() != null
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return auth.getName();
        }
        return "anonyme";
    }
}