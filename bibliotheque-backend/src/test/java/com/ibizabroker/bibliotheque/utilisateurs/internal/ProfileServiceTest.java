package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de ProfileService (GET /profile et /me) : repository simulé,
 * jetons construits à la main, aucun Keycloak.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ProfileService service;

    @Test
    void profilCourant_jetonKeycloak_renvoieLUtilisateurLocalLEmailEtLesRolesTries() {
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(utilisateur("User", "ADHERENT")));
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "a1");
        claims.put("email", "a1@bibliotheque.local");
        Jwt jwt = new Jwt("jeton", Instant.now(), Instant.now().plusSeconds(60),
                Collections.singletonMap("alg", "none"), claims);

        ProfileResponse profil = service.profilCourant(new JwtAuthenticationToken(jwt, Collections.emptyList(), "a1"));

        assertThat(profil.getUserId()).isEqualTo(2);
        assertThat(profil.getUsername()).isEqualTo("a1");
        assertThat(profil.getName()).isEqualTo("Adhérent Un");
        assertThat(profil.getEmail()).isEqualTo("a1@bibliotheque.local");
        assertThat(profil.getRoles()).containsExactly("ADHERENT", "User");
    }

    @Test
    void profilCourant_authentificationSansJwt_renvoieUnEmailNul() {
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(utilisateur("ADHERENT")));

        ProfileResponse profil = service.profilCourant(new TestingAuthenticationToken("a1", null));

        assertThat(profil.getEmail()).isNull();
    }

    @Test
    void profilCourant_utilisateurSansRoles_renvoieUneListeVide() {
        Users sansRoles = utilisateur();
        sansRoles.setRole(null);
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(sansRoles));

        assertThat(service.profilCourant(new TestingAuthenticationToken("a1", null)).getRoles()).isEmpty();
    }

    @Test
    void profilCourant_compteAbsentDeLaBaseLocale_lanceNotFound() {
        when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.profilCourant(new TestingAuthenticationToken("ghost", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost");
    }

    private Users utilisateur(String... roles) {
        Users user = new Users();
        user.setUserId(2);
        user.setUsername("a1");
        user.setName("Adhérent Un");
        Set<Role> ensemble = new HashSet<>();
        for (String nom : roles) {
            Role role = new Role();
            role.setRoleName(nom);
            ensemble.add(role);
        }
        user.setRole(ensemble);
        return user;
    }
}
