package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.UserCreateRequest;
import com.ibizabroker.bibliotheque.dto.UserResponse;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires d'AdminController (gestion des utilisateurs). La logique
 * métier est portée par le contrôleur (pas de couche service) : repository et
 * encodeur de mot de passe sont simulés, aucune base ni contexte Spring.
 * La sécurité par rôle est couverte par UsersAdminApiIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminController controller;

    // ------------------------------------------------------------------
    // Création
    // ------------------------------------------------------------------
    @Test
    void addUserByAdmin_usernameLibre_encodeLeMotDePasseEtCreeLesRoles() {
        when(usersRepository.findByUsername("nouveau")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret1")).thenReturn("hash-bcrypt");
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Users cree = controller.addUserByAdmin(demande("nouveau", "Nouveau", "secret1", "Admin", "User"));

        assertThat(cree.getUsername()).isEqualTo("nouveau");
        assertThat(cree.getName()).isEqualTo("Nouveau");
        assertThat(cree.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(nomsDesRoles(cree)).containsExactlyInAnyOrder("Admin", "User");
    }

    @Test
    void addUserByAdmin_sansRoles_creeUnUtilisateurSansRole() {
        when(usersRepository.findByUsername("nouveau")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret1")).thenReturn("hash-bcrypt");
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserCreateRequest sansRoles = demande("nouveau", "Nouveau", "secret1");
        sansRoles.setRoles(null);

        Users cree = controller.addUserByAdmin(sansRoles);

        assertThat(cree.getRole()).isEmpty();
    }

    @Test
    void addUserByAdmin_usernameDejaUtilise_lanceConflictSansEncoderNiSauvegarder() {
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(utilisateur(2, "a1", "ancien-hash", "ADHERENT")));

        assertThatThrownBy(() -> controller.addUserByAdmin(demande("a1", "Doublon", "secret1")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("a1")
                .hasMessageContaining("déjà utilisé");

        verify(usersRepository, never()).save(any(Users.class));
        verifyNoInteractions(passwordEncoder);
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------
    @Test
    void getAllUsers_appliqueLaPaginationEtLeTriDemandesPuisConvertitEnDto() {
        when(usersRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(utilisateur(2, "a1", "hash", "ADHERENT"))));

        Page<UserResponse> page = controller.getAllUsers(1, 5, "username");

        ArgumentCaptor<Pageable> pagination = ArgumentCaptor.forClass(Pageable.class);
        verify(usersRepository).findAll(pagination.capture());
        assertThat(pagination.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pagination.getValue().getPageSize()).isEqualTo(5);
        assertThat(pagination.getValue().getSort().getOrderFor("username")).isNotNull();

        UserResponse dto = page.getContent().get(0);
        assertThat(dto.getUserId()).isEqualTo(2L);
        assertThat(dto.getUsername()).isEqualTo("a1");
        assertThat(dto.getRoles()).containsExactly("ADHERENT");
    }

    @Test
    void getUserById_utilisateurExistant_renvoie200AvecLeDto() {
        when(usersRepository.findById(2)).thenReturn(Optional.of(utilisateur(2, "a1", "hash", "ADHERENT")));

        ResponseEntity<UserResponse> reponse = controller.getUserById(2);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().getUsername()).isEqualTo("a1");
    }

    @Test
    void getUserById_utilisateurInconnu_lanceNotFound() {
        when(usersRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getUserById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    // ------------------------------------------------------------------
    // Modification
    // ------------------------------------------------------------------
    @Test
    void updateUser_utilisateurInconnu_lanceNotFoundSansSauvegarder() {
        when(usersRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.updateUser(999, demande("x", "X", null)))
                .isInstanceOf(NotFoundException.class);

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updateUser_usernamePrisParUnAutreUtilisateur_lanceConflictSansSauvegarder() {
        when(usersRepository.findById(2)).thenReturn(Optional.of(utilisateur(2, "a1", "hash", "ADHERENT")));
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(utilisateur(1, "admin", "hash", "Admin")));

        assertThatThrownBy(() -> controller.updateUser(2, demande("admin", "Usurpation", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("admin");

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updateUser_enGardantSonPropreUsername_metAJourNomEtRoles() {
        Users a1 = utilisateur(2, "a1", "ancien-hash", "ADHERENT");
        when(usersRepository.findById(2)).thenReturn(Optional.of(a1));
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(a1));
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<UserResponse> reponse = controller.updateUser(2, demande("a1", "Nouveau Nom", null, "User"));

        assertThat(reponse.getBody().getName()).isEqualTo("Nouveau Nom");
        assertThat(reponse.getBody().getRoles()).containsExactly("User");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_motDePasseVide_conserveLeHashExistant() {
        when(usersRepository.findById(2)).thenReturn(Optional.of(utilisateur(2, "a1", "ancien-hash", "ADHERENT")));
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.empty());
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.updateUser(2, demande("a1", "A1", ""));

        ArgumentCaptor<Users> sauvegarde = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(sauvegarde.capture());
        assertThat(sauvegarde.getValue().getPassword()).isEqualTo("ancien-hash");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_motDePasseRenseigne_encodeLeNouveauMotDePasse() {
        when(usersRepository.findById(2)).thenReturn(Optional.of(utilisateur(2, "a1", "ancien-hash", "ADHERENT")));
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("nouveau-secret")).thenReturn("nouveau-hash");
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.updateUser(2, demande("a1", "A1", "nouveau-secret"));

        ArgumentCaptor<Users> sauvegarde = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(sauvegarde.capture());
        assertThat(sauvegarde.getValue().getPassword()).isEqualTo("nouveau-hash");
    }

    @Test
    void updateUser_sansRoles_retireTousLesRoles() {
        when(usersRepository.findById(2)).thenReturn(Optional.of(utilisateur(2, "a1", "hash", "ADHERENT")));
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.empty());
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserCreateRequest sansRoles = demande("a1", "A1", null);
        sansRoles.setRoles(null);

        ResponseEntity<UserResponse> reponse = controller.updateUser(2, sansRoles);

        assertThat(reponse.getBody().getRoles()).isEmpty();
    }

    // ------------------------------------------------------------------

    private UserCreateRequest demande(String username, String name, String password, String... roles) {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(username);
        request.setName(name);
        request.setPassword(password);
        request.setRoles(new HashSet<>(Arrays.asList(roles)));
        return request;
    }

    private Users utilisateur(int userId, String username, String hash, String roleName) {
        Users user = new Users();
        user.setUserId(userId);
        user.setUsername(username);
        user.setName(username);
        user.setPassword(hash);
        Role role = new Role();
        role.setRoleName(roleName);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRole(roles);
        return user;
    }

    private Set<String> nomsDesRoles(Users user) {
        return user.getRole().stream().map(Role::getRoleName).collect(Collectors.toSet());
    }
}
