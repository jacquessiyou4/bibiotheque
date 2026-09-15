package com.ibizabroker.bibliotheque.utilisateurs.api;

import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Ce que la fonctionnalité utilisateurs offre aux autres (emprunts,
 * réservations, données personnelles). Elles n'accèdent jamais à ses entités
 * ni à ses repositories.
 */
public interface UtilisateursApi {

    Optional<UtilisateurResume> utilisateur(Integer userId);

    /** Compte local correspondant au username d'un jeton Keycloak. */
    Optional<UtilisateurResume> parUsername(String username);

    /** Utilisateurs par identifiant, en une requête (les identifiants inconnus sont absents de la carte). */
    Map<Integer, UtilisateurResume> utilisateurs(Collection<Integer> userIds);

    /** Profil de l'utilisateur authentifié. */
    ProfileResponse profilCourant(Authentication authentication);

    /** Retire d'un compte tout ce qui identifie la personne (irréversible). */
    UserResponse anonymiser(Integer userId);
}
