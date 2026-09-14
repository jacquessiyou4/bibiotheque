package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Tests d'intégration de JwtController : endpoint legacy /authenticate et
 * profil de l'utilisateur connecté /profile.
 * Le JwtService, le décodeur JWT et les repositories sont simulés.
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
class JwtControllerIntegrationTest {

    private static final String TOKEN_ADHERENT = "token-adherent";
    private static final String TOKEN_INCONNU = "token-inconnu";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Users utilisateur;

    @BeforeEach
    void setUp() {
        Role roleAdherent = new Role();
        roleAdherent.setRoleName("ADHERENT");
        Role roleUser = new Role();
        roleUser.setRoleName("User");
        Set<Role> roles = new HashSet<>();
        roles.add(roleAdherent);
        roles.add(roleUser);

        utilisateur = new Users();
        utilisateur.setUserId(1);
        utilisateur.setUsername("A1");
        utilisateur.setName("Adherent Un");
        utilisateur.setPassword("hash");
        utilisateur.setRole(roles);

        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(utilisateur));

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if (TOKEN_INCONNU.equals(token)) {
                return jwt("ghost", Collections.singletonList("User"), token);
            }
            return jwt("A1", Arrays.asList("User", "ADHERENT"), token);
        });
    }

    @Test
    void postAuthenticate_avecIdentifiantsValides_renvoieLeJetonEtLUtilisateur() throws Exception {
        JwtResponse response = new JwtResponse(utilisateur, "fake-jwt-token");
        when(jwtService.createJwtToken(any(JwtRequest.class))).thenReturn(response);

        JwtRequest request = new JwtRequest();
        request.setUserName("A1");
        request.setUserPassword("mot-de-passe");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.user.username").value("A1"))
                .andExpect(jsonPath("$.user.name").value("Adherent Un"));
    }
    @Test
    void postAuthenticate_avecMauvaisMotDePasse_renvoieErreur() throws Exception {
        when(jwtService.createJwtToken(any(JwtRequest.class)))
                .thenThrow(new Exception("INVALID_CREDENTIALS"));

        JwtRequest request = new JwtRequest();
        request.setUserName("A1");
        request.setUserPassword("faux");

        try {
            mockMvc.perform(post("/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            org.assertj.core.api.Assertions.assertThat(e.getCause())
                    .hasMessageContaining("INVALID_CREDENTIALS");
        }
    }

    @Test
    void sansToken_profile_renvoie401() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void avecToken_profile_renvoieLesInformationsPersonnelles() throws Exception {
        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("A1"))
                .andExpect(jsonPath("$.name").value("Adherent Un"))
                .andExpect(jsonPath("$.email").value("A1@bibliotheque.local"))
                .andExpect(jsonPath("$.roles[0]").value("ADHERENT"))
                .andExpect(jsonPath("$.roles[1]").value("User"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void avecTokenDUnUtilisateurAbsentDuReferentiel_profile_renvoie404() throws Exception {
        when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_INCONNU))
                .andExpect(status().isNotFound());
    }

    @Test
    void profile_ignoreLeParametreUsername_lIdentiteVientDuToken() throws Exception {
        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("A1"));
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
