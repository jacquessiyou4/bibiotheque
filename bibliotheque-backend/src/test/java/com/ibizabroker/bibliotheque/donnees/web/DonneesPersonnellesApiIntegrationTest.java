package com.ibizabroker.bibliotheque.donnees.web;

import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.Borrow;
import com.ibizabroker.bibliotheque.utilisateurs.internal.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints RGPD : export par l'adhérent de ses propres données, anonymisation
 * réservée à l'administrateur. Décodeur JWT et repositories simulés.
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
class DonneesPersonnellesApiIntegrationTest {

    private static final String BEARER_ADHERENT = "Bearer token-adherent";
    private static final String BEARER_ADMIN = "Bearer token-admin";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        Users adherent = new Users();
        adherent.setUserId(1);
        adherent.setUsername("A1");
        adherent.setName("Adherent Un");
        adherent.setPassword("hash-bcrypt");
        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(adherent));
        when(usersRepository.findById(1)).thenReturn(Optional.of(adherent));
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(10);
        emprunt.setBookId(3);
        emprunt.setUserId(1);
        emprunt.setIssueDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        when(borrowRepository.findByUserId(1)).thenReturn(Collections.singletonList(emprunt));
        when(reservationRepository.findByAdherentId(1)).thenReturn(Collections.emptyList());

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            boolean admin = BEARER_ADMIN.equals("Bearer " + invocation.getArgument(0));
            return jwt(admin ? "admin" : "A1",
                    admin ? Arrays.asList("Admin", "BIBLIOTHECAIRE") : Arrays.asList("User", "ADHERENT"),
                    invocation.getArgument(0));
        });
    }

    @Test
    void sansJeton_export_renvoie401() throws Exception {
        mockMvc.perform(get("/profile/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adherent_exporteSesDonneesEnFichierJson() throws Exception {
        mockMvc.perform(get("/profile/export").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("mes-donnees-bibliotheque.json")))
                .andExpect(jsonPath("$.profil.userId").value(1))
                .andExpect(jsonPath("$.profil.password").doesNotExist())
                .andExpect(jsonPath("$.emprunts", hasSize(1)))
                .andExpect(jsonPath("$.emprunts[0].issueDate").value("2026-09-01T10:00:00"))
                .andExpect(jsonPath("$.reservations", hasSize(0)))
                .andExpect(jsonPath("$.exporteLe").exists());
    }

    @Test
    void admin_anonymiseUnCompte() throws Exception {
        mockMvc.perform(post("/admin/users/1/anonymisation").header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("anonyme-1"))
                .andExpect(jsonPath("$.roles", hasSize(0)));

        ArgumentCaptor<Users> sauvegarde = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(sauvegarde.capture());
        assertThat(sauvegarde.getValue().getPassword()).isNull();
        assertThat(sauvegarde.getValue().getName()).isEqualTo("Utilisateur anonymisé");
    }

    @Test
    void adherent_nePeutPasAnonymiser() throws Exception {
        mockMvc.perform(post("/admin/users/1/anonymisation").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isForbidden());

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void admin_anonymiseUnCompteInconnu_renvoie404() throws Exception {
        mockMvc.perform(post("/admin/users/99/anonymisation").header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
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
