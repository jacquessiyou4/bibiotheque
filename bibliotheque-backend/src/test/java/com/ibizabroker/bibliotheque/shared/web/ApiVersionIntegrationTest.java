package com.ibizabroker.bibliotheque.shared.web;

import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chemins versionnés (/api/v1) et anciens chemins, avec la vraie chaîne de
 * filtres : même réponse, mêmes règles d'accès, en-têtes d'obsolescence sur
 * les anciens chemins seulement.
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
class ApiVersionIntegrationTest {

    private static final String BEARER_ADHERENT = "Bearer token-adherent";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        Users adherent = new Users();
        adherent.setUserId(1);
        adherent.setUsername("A1");
        adherent.setName("Adherent Un");
        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(adherent));
        when(borrowRepository.findByUserId(1)).thenReturn(Collections.emptyList());
        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", Arrays.asList("User", "ADHERENT"));
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "A1");
            claims.put("preferred_username", "A1");
            claims.put("realm_access", realmAccess);
            return new Jwt(invocation.getArgument(0), Instant.now(), Instant.now().plusSeconds(300),
                    Collections.singletonMap("alg", "none"), claims);
        });
    }

    @Test
    void cheminVersionne_repondSansEnTeteDObsolescence() throws Exception {
        mockMvc.perform(get("/api/v1/loans/user/1").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"));
    }

    @Test
    void ancienChemin_repondPareilAvecLesEnTetesDObsolescence() throws Exception {
        mockMvc.perform(get("/borrow/user/1").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Mon, 15 Mar 2027 00:00:00 GMT"))
                .andExpect(header().string("Link", "</api/v1/loans/user/1>; rel=\"successor-version\""));
    }

    @Test
    void ancienChemin_resteSoumisAuxMemesReglesDAcces() throws Exception {
        // Un adhérent ne lit pas les emprunts d'un autre, quel que soit le chemin utilisé.
        mockMvc.perform(get("/borrow/user/2").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BORROW_NOT_OWNED"));
        mockMvc.perform(get("/borrow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.instance").value("/api/v1/loans"));
    }

    @Test
    void profil_accessibleSousLesDeuxChemins() throws Exception {
        mockMvc.perform(get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
        mockMvc.perform(get("/profile").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"));
    }
}
