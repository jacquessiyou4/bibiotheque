package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Profil de l'utilisateur connecté (GET /profile, et GET /me conservé pour
 * compatibilité). L'identité vient toujours du jeton Keycloak
 * (preferred_username), jamais d'un paramètre de requête.
 */
@Service
public class ProfileService {

    private final UsersRepository usersRepository;

    public ProfileService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public ProfileResponse profilCourant(Authentication authentication) {
        String username = authentication.getName();
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("LOCAL_ACCOUNT_MISSING",
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
