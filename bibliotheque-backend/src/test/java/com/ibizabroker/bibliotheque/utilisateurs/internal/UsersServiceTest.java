package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UserResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateurResume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UsersService service;

    @Test
    void utilisateur_etParUsername_renvoientUnResumeSansDonneeSensible() {
        when(usersRepository.findById(2)).thenReturn(Optional.of(compte()));
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(compte()));

        UtilisateurResume parId = service.utilisateur(2).orElseThrow(AssertionError::new);
        UtilisateurResume parNom = service.parUsername("a1").orElseThrow(AssertionError::new);

        assertThat(parId.getName()).isEqualTo("Adhérent Un");
        assertThat(parNom.getUserId()).isEqualTo(2);
        assertThat(parNom.getUsername()).isEqualTo("a1");
    }

    @Test
    void utilisateurs_chargeLesComptesEnUneRequete() {
        when(usersRepository.findAllById(Arrays.asList(2, 3))).thenReturn(Collections.singletonList(compte()));

        Map<Integer, UtilisateurResume> comptes = service.utilisateurs(Arrays.asList(2, 3));

        assertThat(comptes).containsOnlyKeys(2);
    }

    @Test
    void utilisateurs_sansIdentifiant_nInterrogePasLaBase() {
        assertThat(service.utilisateurs(Collections.emptyList())).isEmpty();
        verify(usersRepository, never()).findAllById(any());
    }

    @Test
    void profilCourant_delegueAuServiceDeProfil() {
        Authentication a1 = new TestingAuthenticationToken("a1", null);
        ProfileResponse profil = new ProfileResponse(2, "a1", "Adhérent Un", null, Collections.emptyList());
        when(profileService.profilCourant(a1)).thenReturn(profil);

        assertThat(service.profilCourant(a1)).isSameAs(profil);
    }

    @Test
    void anonymiser_retireToutCeQuiIdentifieLaPersonne() {
        Users compte = compte();
        compte.setPassword("hash");
        compte.setKeycloakSub("5b0c7a1e-sub");
        Role role = new Role();
        role.setRoleName("ADHERENT");
        compte.setRole(new HashSet<>(Collections.singletonList(role)));
        when(usersRepository.findById(2)).thenReturn(Optional.of(compte));
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse anonyme = service.anonymiser(2);

        assertThat(anonyme.getUserId()).isEqualTo(2);
        assertThat(anonyme.getUsername()).isEqualTo("anonyme-2");
        assertThat(anonyme.getName()).isEqualTo(UsersService.NOM_ANONYME);
        assertThat(anonyme.getRoles()).isEmpty();
        assertThat(compte.getPassword()).isNull();
        assertThat(compte.getKeycloakSub()).isNull();
        assertThat(compte.getRole()).isEmpty();
    }

    @Test
    void anonymiser_compteInconnu_lanceNotFoundSansRienEnregistrer() {
        when(usersRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anonymiser(99)).isInstanceOf(NotFoundException.class);
        verify(usersRepository, never()).save(any(Users.class));
    }

    private Users compte() {
        Users user = new Users();
        user.setUserId(2);
        user.setUsername("a1");
        user.setName("Adhérent Un");
        return user;
    }
}
