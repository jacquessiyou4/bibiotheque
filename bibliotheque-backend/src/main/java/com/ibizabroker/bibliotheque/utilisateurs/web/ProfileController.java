package com.ibizabroker.bibliotheque.utilisateurs.web;

import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.utilisateurs.internal.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification")
@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Profil de la personne connectée. L'identité vient toujours du jeton
     * Keycloak (preferred_username), jamais d'un paramètre de requête :
     * on ne peut pas consulter le profil d'un autre utilisateur.
     */
    @Operation(summary = "Obtenir les informations personnelles de l'utilisateur connecté")
    @GetMapping("/api/v1/profile")
    public ProfileResponse profile(Authentication authentication) {
        return profileService.profilCourant(authentication);
    }
}
