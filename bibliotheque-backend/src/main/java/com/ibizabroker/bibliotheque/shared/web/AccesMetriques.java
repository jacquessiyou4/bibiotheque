package com.ibizabroker.bibliotheque.shared.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Accès de Prometheus à /actuator/prometheus. Prometheus ne peut pas obtenir
 * de jeton Keycloak : il présente un identifiant et un mot de passe HTTP Basic
 * (ops/observability/prometheus.yml). Sans mot de passe configuré, cet accès
 * est fermé et seul un administrateur authentifié lit les métriques.
 *
 * Utilisé par WebSecurityConfiguration : {@code access("@accesMetriques.autorise(request) or hasRole('Admin')")}.
 */
@Component("accesMetriques")
public class AccesMetriques {

    private final byte[] identifiantsAttendus;

    public AccesMetriques(@Value("${app.metriques.utilisateur:prometheus}") String utilisateur,
                          @Value("${app.metriques.mot-de-passe:}") String motDePasse) {
        this.identifiantsAttendus = motDePasse.isEmpty()
                ? null
                : (utilisateur + ":" + motDePasse).getBytes(StandardCharsets.UTF_8);
    }

    public boolean autorise(HttpServletRequest request) {
        if (identifiantsAttendus == null) {
            return false;
        }
        String entete = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete == null || !entete.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        byte[] fournis;
        try {
            fournis = Base64.getDecoder().decode(entete.substring(6).trim());
        } catch (IllegalArgumentException base64Invalide) {
            return false;
        }
        // Comparaison en temps constant : le délai de réponse ne révèle rien du mot de passe.
        return MessageDigest.isEqual(identifiantsAttendus, fournis);
    }
}
