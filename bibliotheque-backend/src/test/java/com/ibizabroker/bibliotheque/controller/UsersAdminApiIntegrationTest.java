package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de la gestion des utilisateurs (endpoints /admin/users)
 * : sécurisation par rôle (401/403) et encodage BCrypt du mot de passe à la
 * création. Décodeur JWT et repositories simulés (aucune base nécessaire).
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "app.keycloak.jwks-uri=http://localhost:9999/realms/bibliotheque/protocol/openid-connect/certs",
        "app.keycloak.issuer-local=http://localhost:9999/realms/bibliotheque",
        "app.keycloak.issuer-internal=http://localhost:9999/realms/bibliotheque"
})
@AutoConfigureMockMvc
class UsersAdminApiIntegrationTest {

    private static final String TOKEN_ADHERENT = "token-adherent";
    private static final String TOKEN_ADMIN = "token-admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Users adherent;
    private Users bibliothecaire;

    @BeforeEach
    void setUp() {
        adherent = utilisateur(1, "A1", "Adherent");
        bibliothecaire = utilisateur(2, "admin", "Admin");

        when(usersRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(adherent, bibliothecaire)));
        when(usersRepository.findById(1)).thenReturn(Optional.of(adherent));
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if (TOKEN_ADMIN.equals(token)) {
                return jwt("admin", Arrays.asList("Admin", "BIBLIOTHECAIRE"), token);
            }
            return jwt("A1", Arrays.asList("User", "ADHERENT"), token);
        });
    }

    // ------------------------------------------------------------------
    // Sans token -> 401
    // ------------------------------------------------------------------
    @Test
    void sansToken_getUsers_renvoie401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sansToken_postUser_renvoie401() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Un ADHERENT ne peut pas gérer les utilisateurs (403)
    // ------------------------------------------------------------------
    @Test
    void avecTokenAdherent_getUsers_renvoie403() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void avecTokenAdherent_getUserById_renvoie403() throws Exception {
        mockMvc.perform(get("/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void avecTokenAdherent_putUser_renvoie403() throws Exception {
        mockMvc.perform(put("/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        // Corps valide : @Valid passe avant @PreAuthorize (sinon 400).
                        .content("{\"username\":\"X\",\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void avecTokenAdherent_postUser_renvoie403EtNeSauvegardeRien() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nouveau\",\"name\":\"Nouveau\","
                                + "\"password\":\"mot-de-passe-clair\",\"roles\":[]}"))
                .andExpect(status().isForbidden());

        verify(usersRepository, never()).save(any(Users.class));
    }

    // ------------------------------------------------------------------
    // L'Admin liste et consulte (réponse paginée, DTO UserResponse)
    // ------------------------------------------------------------------
    @Test
    void avecTokenAdmin_getUsers_renvoieLaPageDesUtilisateurs() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].username").value("A1"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("Adherent"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void avecTokenAdmin_getUserById_renvoie200() throws Exception {
        mockMvc.perform(get("/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("A1"));
    }

    @Test
    void avecTokenAdmin_getUserInconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/admin/users/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Création : le mot de passe est encodé (jamais stocké/renvoyé en clair)
    // ------------------------------------------------------------------
    @Test
    void avecTokenAdmin_postUser_encodeLeMotDePasseAvantSauvegarde() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nouveau\",\"name\":\"Nouveau\","
                                + "\"password\":\"mot-de-passe-clair\",\"roles\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nouveau"))
                // @JsonIgnore : le hash n'est jamais renvoyé au client.
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<Users> sauvegarde = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(sauvegarde.capture());
        String hash = sauvegarde.getValue().getPassword();
        assertThat(hash).startsWith("$2").isNotEqualTo("mot-de-passe-clair");
        assertThat(passwordEncoder.matches("mot-de-passe-clair", hash)).isTrue();
    }

    @Test
    void avecTokenAdmin_putUser_metAjourNomUsernameEtRole() throws Exception {
        mockMvc.perform(put("/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"A1-modifié\",\"name\":\"Nom Modifié\","
                                + "\"roles\":[\"Admin\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nom Modifié"))
                .andExpect(jsonPath("$.username").value("A1-modifié"))
                .andExpect(jsonPath("$.roles[0]").value("Admin"));
    }

    // ------------------------------------------------------------------

    private Users utilisateur(int userId, String username, String roleName) {
        Users user = new Users();
        user.setUserId(userId);
        user.setUsername(username);
        user.setName(username);
        user.setPassword("hash-bcrypt");
        Role role = new Role();
        role.setRoleId(userId);
        role.setRoleName(roleName);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRole(roles);
        return user;
    }

    private Jwt jwt(String username, List<String> roles, String token) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("preferred_username", username);
        claims.put("realm_access", realmAccess);
        return new Jwt(token, Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);
    }
}
