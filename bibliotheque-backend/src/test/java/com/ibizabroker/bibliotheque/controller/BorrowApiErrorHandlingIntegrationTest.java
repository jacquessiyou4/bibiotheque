package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
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
import java.util.Map;
import java.util.Optional;

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
 * Tests d'intégration supplémentaires pour BorrowController : gestion des
 * erreurs, cas limites (emprunt hors stock, utilisateur/livre inexistant,
 * corps invalide). Les erreurs métier sont traduites en réponses HTTP par
 * GlobalExceptionHandler (404 / 400) au lieu de remonter en exception.
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
class BorrowApiErrorHandlingIntegrationTest {

    private static final String BEARER_ADHERENT = "Bearer token-adherent";
    // Seul le personnel peut emprunter pour un autre utilisateur / lire
    // l'historique d'un livre : les cas 404 de ces endpoints passent par lui.
    private static final String BEARER_ADMIN = "Bearer token-admin";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private UsersRepository usersRepository;

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

        Books livre = new Books();
        livre.setBookId(3);
        livre.setBookName("L2");
        livre.setNoOfCopies(2);

        when(usersRepository.findById(1)).thenReturn(Optional.of(adherent));
        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(adherent));
        when(booksRepository.findById(3)).thenReturn(Optional.of(livre));
        when(booksRepository.save(any(Books.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            boolean admin = BEARER_ADMIN.equals("Bearer " + invocation.getArgument(0));
            String username = admin ? "admin" : "A1";
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", admin ? Arrays.asList("Admin", "BIBLIOTHECAIRE") : Arrays.asList("User", "ADHERENT"));
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", username);
            claims.put("preferred_username", username);
            claims.put("realm_access", realmAccess);
            return new Jwt(invocation.getArgument(0), Instant.now(), Instant.now().plusSeconds(300),
                    Collections.singletonMap("alg", "none"), claims);
        });
    }

    @Test
    void postBorrow_utilisateurInexistant_renvoie404() throws Exception {
        when(usersRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"bookId\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void postBorrow_livreInexistant_renvoie404() throws Exception {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"bookId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Livre introuvable"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void postBorrow_sansBookId_renvoie400DeValidation() throws Exception {
        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.bookId").exists());
    }

    @Test
    void postBorrow_stockEpuise_renvoie400EtNeSauvegardePasLaCopie() throws Exception {
        Books livreEpuise = new Books();
        livreEpuise.setBookId(3);
        livreEpuise.setBookName("L2 Epuise");
        livreEpuise.setNoOfCopies(0);
        when(booksRepository.findById(3)).thenReturn(Optional.of(livreEpuise));

        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("n'est plus disponible")));

        verify(booksRepository, never()).save(any(Books.class));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void putBorrow_empruntInexistant_renvoie404() throws Exception {
        when(borrowRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":999,\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Emprunt introuvable"));
    }

    @Test
    void getBorrowsByUser_renvoieListeVideSiAucunEmprunt() throws Exception {
        when(borrowRepository.findByUserId(999)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/borrow/user/999").header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBorrowsByBook_renvoieListeVideSiAucunEmprunt() throws Exception {
        when(borrowRepository.findByBookId(999)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/borrow/book/999").header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBorrowsByUser_idNonNumerique_renvoie400() throws Exception {
        mockMvc.perform(get("/borrow/user/abc").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postBorrow_sauvegardeLesDatesIssueEtDue() throws Exception {
        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":10,\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Borrow> captor = org.mockito.ArgumentCaptor.forClass(Borrow.class);
        verify(borrowRepository).save(captor.capture());
        Borrow emprunt = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(emprunt.getIssueDate()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(emprunt.getDueDate()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(emprunt.getReturnDate()).isNull();
    }
}
