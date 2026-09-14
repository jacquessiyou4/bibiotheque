package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ProfileResponse;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Authentification")
@Slf4j
@RestController
public class JwtController {

    private final JwtService jwtService;
    private final UsersRepository usersRepository;

    public JwtController(JwtService jwtService, UsersRepository usersRepository) {
        this.jwtService = jwtService;
        this.usersRepository = usersRepository;
    }

    @Operation(summary = "Authentifier un utilisateur et obtenir un jeton JWT")
    @PostMapping("/authenticate")
    public JwtResponse createJwtToken(@RequestBody JwtRequest jwtRequest) throws Exception {
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
        String username = authentication.getName();
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun utilisateur local pour le compte Keycloak « " + username + " »."));

        String email = null;
        if (authentication instanceof JwtAuthenticationToken) {
            email = ((JwtAuthenticationToken) authentication).getToken().getClaimAsString("email");
        }

        List<String> roles = user.getRole() == null ? Collections.emptyList()
                : user.getRole().stream()
                        .map(Role::getRoleName)
                        .sorted()
                        .collect(Collectors.toList());

        return new ProfileResponse(user.getUserId(), user.getUsername(), user.getName(), email, roles);
    }
}
