package com.ibizabroker.bibliotheque.utilisateurs.web;

import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.Role;
import com.ibizabroker.bibliotheque.utilisateurs.internal.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration du profil /profile : l'identité vient toujours du
 * token (claim preferred_username), jamais d'un paramètre de requête.
 * Décodeur JWT et repositories simulés (aucune base nécessaire).
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
class ProfileApiIntegrationTest {

    private static final String TOKEN_ADHERENT = "token-adherent";
    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_INCONNU = "token-inconnu";

    @Autowired
    private MockMvc mockMvc;

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

    @BeforeEach
    void setUp() {
        Users adherent = new Users();
        adherent.setUserId(1);
        adherent.setUsername("A1");
        adherent.setName("Adherent Un");
        adherent.setPassword("hash-bcrypt");
        Role roleAdherent = new Role();
        roleAdherent.setRoleName("ADHERENT");
        Role roleUser = new Role();
        roleUser.setRoleName("User");
        Set<Role> roles = new HashSet<>();
        roles.add(roleAdherent);
        roles.add(roleUser);
        adherent.setRole(roles);

        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(adherent));

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            switch (token) {
                case TOKEN_ADMIN:
                    return jwt("admin", Arrays.asList("Admin", "BIBLIOTHECAIRE"), token);
                case TOKEN_INCONNU:
                    return jwt("ghost", Collections.singletonList("User"), token);
                default:
                    return jwt("A1", Arrays.asList("User", "ADHERENT"), token);
            }
        });
    }

    @Test
    void sansToken_profile_renvoie401() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void avecTokenAdherent_profile_renvoieLesInformationsPersonnelles() throws Exception {
        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("A1"))
                .andExpect(jsonPath("$.name").value("Adherent Un"))
                .andExpect(jsonPath("$.email").value("A1@bibliotheque.local"))
                .andExpect(jsonPath("$.roles[0]").value("ADHERENT"))
                .andExpect(jsonPath("$.roles[1]").value("User"))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void avecTokenAdmin_profile_renvoieLIdentiteAdmin() throws Exception {
        Users bibliothecaire = new Users();
        bibliothecaire.setUserId(10);
        bibliothecaire.setUsername("admin");
        bibliothecaire.setName("Bibliothecaire");
        Role roleAdmin = new Role();
        roleAdmin.setRoleName("BIBLIOTHECAIRE");
        Set<Role> roles = new HashSet<>();
        roles.add(roleAdmin);
        bibliothecaire.setRole(roles);
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(bibliothecaire));

        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void avecTokenDUnUtilisateurAbsentDuReferentiel_profile_renvoie404() throws Exception {
        // « ghost » est présent dans le token mais plus dans la base : pas de
        // fuite d'identité, la requête est rejetée en 404.
        when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_INCONNU))
                .andExpect(status().isNotFound());
    }

    @Test
    void profile_ignoreLeParametreUsername_lIdentiteVientDuToken() throws Exception {
        // Un appelant ne peut pas se faire passer pour un autre utilisateur
        // en ajoutant ?username=admin : le paramètre est ignoré.
        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("A1"));
    }

    @Test
    void anciensEndpointsSupprimes_neSontPlusAccessibles() throws Exception {
        // POST /authenticate (login jjwt) et GET /me ont été retirés : sans
        // jeton, ils ne sont plus ouverts et répondent 401 comme toute route inconnue.
        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------

    private Jwt jwt(String username, List<String> roles, String token) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("preferred_username", username);
        claims.put("email", username + "@bibliotheque.local");
        claims.put("realm_access", realmAccess);
        return new Jwt(token, Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);
    }
}
