package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ProfileResponse;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.service.JwtService;
import com.ibizabroker.bibliotheque.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification")
@Slf4j
@RestController
public class JwtController {

    private final JwtService jwtService;
    private final ProfileService profileService;

    public JwtController(JwtService jwtService, ProfileService profileService) {
        this.jwtService = jwtService;
        this.profileService = profileService;
    }

    @Operation(summary = "Obsolète : ancien login local, jeton refusé par l'API",
            description = "Le jeton renvoyé n'est pas accepté par les autres endpoints (seuls les jetons "
                    + "Keycloak le sont). Pour tester l'API : bouton « Authorize » > keycloak.",
            deprecated = true)
    @PostMapping("/authenticate")
    public JwtResponse createJwtToken(@RequestBody JwtRequest jwtRequest) throws Exception {
        log.warn("[SECURITE] Appel de l'endpoint obsolète POST /authenticate - utilisateur={}",
                jwtRequest.getUsername());
        return jwtService.createJwtToken(jwtRequest);
    }

    /**
     * Profil de la personne connectée. L'identité vient toujours du jeton
     * Keycloak (preferred_username), jamais d'un paramètre de requête :
     * on ne peut pas consulter le profil d'un autre utilisateur.
     */
    @Operation(summary = "Obtenir les informations personnelles de l'utilisateur connecté")
    @GetMapping("/profile")
    public ProfileResponse profile(Authentication authentication) {
        return profileService.profilCourant(authentication);
    }
}
