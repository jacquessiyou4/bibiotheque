package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.shared.web.ProblemeHttp;
import com.ibizabroker.bibliotheque.shared.util.Canonical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Relie chaque compte local à son compte Keycloak par le claim immuable
 * « sub » du jeton, et non plus par le seul username :
 * <ul>
 *   <li>première requête d'un compte : le sub est enregistré (users.keycloak_sub) ;</li>
 *   <li>username renommé dans Keycloak : le compte local suit le nouveau nom ;</li>
 *   <li>username réattribué à un autre compte Keycloak : 403, le nouveau
 *       titulaire n'hérite pas des emprunts de l'ancien.</li>
 * </ul>
 * Placé juste après la validation du jeton ; les services continuent de
 * retrouver le compte par username, désormais garanti cohérent avec le sub.
 * Un lien déjà vérifié est mémorisé : pas de requête SQL à chaque appel.
 */
public class CompteKeycloakFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CompteKeycloakFilter.class);

    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, String> liensVerifies = new ConcurrentHashMap<>();

    public CompteKeycloakFilter(UsersRepository usersRepository, ObjectMapper objectMapper) {
        this.usersRepository = usersRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            String sub = ((JwtAuthenticationToken) authentication).getToken().getSubject();
            String username = Canonical.username(authentication.getName());
            boolean aVerifier = sub != null && username != null && !username.equals(liensVerifies.get(sub));
            if (aVerifier && !synchroniser(sub, username)) {
                ProblemeHttp.ecrire(response, objectMapper, HttpStatus.FORBIDDEN, "ACCOUNT_LINK_MISMATCH",
                        "Ce compte Keycloak ne correspond pas au compte local « " + username
                                + " ». Contactez un bibliothécaire.",
                        request.getRequestURI());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /** @return false si le jeton ne peut pas être relié au compte local de ce username */
    boolean synchroniser(String sub, String username) {
        Optional<Users> parSub = usersRepository.findByKeycloakSub(sub);
        if (parSub.isPresent()) {
            Users compte = parSub.get();
            if (!username.equals(compte.getUsername())) {
                Optional<Users> homonyme = usersRepository.findByUsername(username);
                if (homonyme.isPresent() && !homonyme.get().getUserId().equals(compte.getUserId())) {
                    log.warn("[SECURITE] Renommage Keycloak '{}' -> '{}' refusé : username déjà pris par userId={}",
                            compte.getUsername(), username, homonyme.get().getUserId());
                    return false;
                }
                usersRepository.renommer(compte.getUserId(), username);
                log.info("[COMPTE] Username renommé dans Keycloak : '{}' -> '{}' (userId={})",
                        compte.getUsername(), username, compte.getUserId());
            }
            liensVerifies.put(sub, username);
            return true;
        }

        Optional<Users> parNom = usersRepository.findByUsername(username);
        if (!parNom.isPresent()) {
            // Pas de compte local : les services répondent eux-mêmes (403 / 404).
            return true;
        }
        Users compte = parNom.get();
        if (compte.getKeycloakSub() == null) {
            usersRepository.lierCompteKeycloak(compte.getUserId(), sub);
            log.info("[COMPTE] Compte local '{}' (userId={}) relié au compte Keycloak {}",
                    username, compte.getUserId(), sub);
            liensVerifies.put(sub, username);
            return true;
        }
        log.warn("[SECURITE] Username '{}' déjà relié à un autre compte Keycloak : accès refusé", username);
        return false;
    }
}
