package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ProfileResponse;
import com.ibizabroker.bibliotheque.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ancien endpoint d'identité, remplacé par GET /profile (le frontend ne
 * l'utilise plus). Conservé pour compatibilité : il renvoie désormais le même
 * DTO que /profile au lieu d'exposer les entités Role.
 */
@Tag(name = "Utilisateur courant")
@Slf4j
@RestController
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Obsolète : utiliser GET /profile",
            description = "Conservé pour compatibilité ; renvoie le même profil que GET /profile.",
            deprecated = true)
    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        return profileService.profilCourant(authentication);
    }
}
