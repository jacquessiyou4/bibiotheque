package com.ibizabroker.bibliotheque.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion (POST /api/v1/auth/token) par adresse IP :
 * au-delà de MAX_TENTATIVES sur une minute glissante, réponse 429 avec
 * Retry-After. Complète la protection de Keycloak, qui verrouille un COMPTE
 * après 5 échecs mais ne freine pas un essai d'un même mot de passe sur
 * beaucoup de comptes différents.
 * Compteurs en mémoire : suffisant pour une instance ; à déplacer vers le
 * proxy (Caddy) ou un stockage partagé si le backend est répliqué.
 */
public class LimiteConnexionFilter extends OncePerRequestFilter {

    static final int MAX_TENTATIVES = 10;
    static final Duration FENETRE = Duration.ofMinutes(1);

    private static final Logger log = LoggerFactory.getLogger(LimiteConnexionFilter.class);

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, Deque<Long>> tentativesParIp = new ConcurrentHashMap<>();

    public LimiteConnexionFilter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && "/api/v1/auth/token".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        long maintenant = clock.millis();
        long attenteMs = enregistrer(ip, maintenant);
        if (attenteMs > 0) {
            long attenteSecondes = Math.max(1, (attenteMs + 999) / 1000);
            log.warn("[SECURITE] Trop de tentatives de connexion depuis {} : refus pendant {} s", ip, attenteSecondes);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(attenteSecondes));
            ProblemeHttp.ecrire(response, objectMapper, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_LOGIN_ATTEMPTS",
                    "Trop de tentatives de connexion. Réessayez dans " + attenteSecondes + " secondes.",
                    request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }

    /** @return 0 si la tentative est acceptée, sinon le délai d'attente en ms */
    long enregistrer(String ip, long maintenant) {
        long debutFenetre = maintenant - FENETRE.toMillis();
        Deque<Long> tentatives = tentativesParIp.computeIfAbsent(ip, cle -> new ArrayDeque<>());
        synchronized (tentatives) {
            while (!tentatives.isEmpty() && tentatives.peekFirst() <= debutFenetre) {
                tentatives.pollFirst();
            }
            if (tentatives.size() >= MAX_TENTATIVES) {
                return tentatives.peekFirst() + FENETRE.toMillis() - maintenant;
            }
            tentatives.addLast(maintenant);
        }
        nettoyer(debutFenetre);
        return 0;
    }

    /** Oublie les adresses sans tentative récente, pour que la table ne grossisse pas indéfiniment. */
    private void nettoyer(long debutFenetre) {
        tentativesParIp.entrySet().removeIf(entree -> {
            Deque<Long> tentatives = entree.getValue();
            synchronized (tentatives) {
                return tentatives.isEmpty() || tentatives.peekLast() <= debutFenetre;
            }
        });
    }
}
