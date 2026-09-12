package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de JwtService (authentification locale « legacy » de
 * /authenticate). L'AuthenticationManager, le repository d'utilisateurs et
 * JwtUtil sont simulés : aucune base ni Keycloak nécessaires.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtServiceTest {

    private static final String LOGIN = "A1";
    private static final String MOT_DE_PASSE = "mot-de-passe";
    private static final String JETON = "jeton-genere";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private JwtService jwtService;

    private Users utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = utilisateurAvecRoles(LOGIN, "Admin", "User");

        when(usersRepository.findByUsername(LOGIN)).thenReturn(Optional.of(utilisateur));
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn(JETON);
    }

    private JwtRequest requete(String username, String password) {
        JwtRequest jwtRequest = new JwtRequest();
        jwtRequest.setUserName(username);
        jwtRequest.setUserPassword(password);
        return jwtRequest;
    }

    @Test
    void createJwtToken_authentifiePuisRenvoieLUtilisateurEtLeJeton() throws Exception {
        JwtResponse response = jwtService.createJwtToken(requete(LOGIN, MOT_DE_PASSE));

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(LOGIN, MOT_DE_PASSE));
        assertThat(response.getJwtToken()).isEqualTo(JETON);
        assertThat(response.getUser()).isEqualTo(utilisateur);
    }

    @Test
    void createJwtToken_genereLeJetonAvecLesInfosDeLUtilisateurCharge() throws Exception {
        jwtService.createJwtToken(requete(LOGIN, MOT_DE_PASSE));

        org.mockito.ArgumentCaptor<UserDetails> captor =
                org.mockito.ArgumentCaptor.forClass(UserDetails.class);
        verify(jwtUtil).generateToken(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo(LOGIN);
        assertThat(captor.getValue().getPassword()).isEqualTo("mot-de-passe-hashé");
    }

    @Test
    void createJwtToken_renvoieINVALID_CREDENTIALS_surMauvaisMotDePasse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("mauvais mot de passe"));

        assertThatThrownBy(() -> jwtService.createJwtToken(requete(LOGIN, "faux")))
                .hasMessage("INVALID_CREDENTIALS");
    }

    @Test
    void createJwtToken_renvoieUSER_DISABLED_surCompteDésactivé() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("compte désactivé"));

        assertThatThrownBy(() -> jwtService.createJwtToken(requete(LOGIN, MOT_DE_PASSE)))
                .hasMessage("USER_DISABLED");
    }

    @Test
    void loadUserByUsername_construitUnUserDetailsAvecLePrefixeROLE() {
        UserDetails userDetails = jwtService.loadUserByUsername(LOGIN);

        assertThat(userDetails.getUsername()).isEqualTo(LOGIN);
        assertThat(userDetails.getAuthorities())
                .extracting(authority -> ((SimpleGrantedAuthority) authority).getAuthority())
                .containsExactlyInAnyOrder("ROLE_Admin", "ROLE_User");
    }

    @Test
    void loadUserByUsername_leveUneExceptionPourUnUtilisateurInconnu() {
        when(usersRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        // L'implémentation actuelle fait Optional.get() sans garde : c'est la
        // NoSuchElementException de l'Optional vide qui remonte.
        assertThatThrownBy(() -> jwtService.loadUserByUsername("inconnu"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ------------------------------------------------------------------

    private Users utilisateurAvecRoles(String username, String... roleNames) {
        Users user = new Users();
        user.setUserId(1);
        user.setUsername(username);
        user.setName("Adherent Un");
        user.setPassword("mot-de-passe-hashé");

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setRoleName(roleName);
            roles.add(role);
        }
        user.setRole(roles);
        return user;
    }
}
