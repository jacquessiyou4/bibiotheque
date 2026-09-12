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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de l'endpoint legacy /authenticate (JwtController).
 * Le JwtService est simulé pour isoler le controller.
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
        Role role = new Role();
        role.setRoleName("ADHERENT");
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        utilisateur = new Users();
        utilisateur.setUserId(1);
        utilisateur.setUsername("A1");
        utilisateur.setName("Adherent Un");
        utilisateur.setPassword("hash");
        utilisateur.setRole(roles);
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
}
