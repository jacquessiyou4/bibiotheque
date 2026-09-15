package com.ibizabroker.bibliotheque.catalogue.web;


import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import com.ibizabroker.bibliotheque.catalogue.internal.Books;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration du CRUD des livres (endpoints /admin/books).
 * Même approche que ReservationControllerIntegrationTest : le décodage JWT
 * est simulé (@MockBean JwtDecoder) et les repositories sont moqués, donc
 * ni Keycloak ni PostgreSQL ne sont nécessaires. La vraie couche de sécurité
 * (WebSecurityConfiguration + @PreAuthorize) et les contrôleurs restent
 * réels : on vérifie la distinction 401 (sans jeton) / 403 (rôle insuffisant).
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
class BooksAdminApiIntegrationTest {

    private static final String TOKEN_ADHERENT = "token-adherent";
    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_EXPIRE = "token-expire";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Books livre;

    @BeforeEach
    void setUp() {
        livre = new Books();
        livre.setBookId(101);
        livre.setBookName("L1");
        livre.setBookAuthor("Auteur 1");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(2);

        when(booksRepository.findById(101)).thenReturn(Optional.of(livre));
        when(booksRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(livre)));
        when(booksRepository.save(any(Books.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if (TOKEN_EXPIRE.equals(token)) {
                throw new JwtException("Jwt expired at 2026-09-01T00:00:00Z, current time is 2026-09-11T00:00:00Z");
            }
            if (TOKEN_ADMIN.equals(token)) {
                return jwt("admin", Arrays.asList("Admin", "BIBLIOTHECAIRE"), token);
            }
            return jwt("A1", Arrays.asList("User", "ADHERENT"), token);
        });
    }

    // ------------------------------------------------------------------
    // Sans token -> 401 sur tout endpoint (même logique que RS-01)
    // ------------------------------------------------------------------
    @Test
    void sansToken_getBooks_renvoie401() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sansToken_getBooksAvecSlashFinal_renvoie401() throws Exception {
        // "/admin/books/" était ouvert (permitAll) dans la configuration de sécurité.
        mockMvc.perform(get("/admin/books/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sansToken_getBookById_renvoie401() throws Exception {
        mockMvc.perform(get("/admin/books/101"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sansToken_postBook_renvoie401() throws Exception {
        mockMvc.perform(post("/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void avecTokenExpiré_getBooks_renvoie401AvecMessageDeSessionExpirée() throws Exception {
        mockMvc.perform(get("/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_EXPIRE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Votre session a expiré. Veuillez vous reconnecter."));
    }

    // ------------------------------------------------------------------
    // Un ADHERENT : la liste est consultable, le CRUD est interdit (403)
    // ------------------------------------------------------------------
    @Test
    void avecTokenAdherent_getBooks_renvoie200EtLaPage() throws Exception {
        mockMvc.perform(get("/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].bookName").value("L1"));
    }

    @Test
    void avecTokenAdherent_getBookById_renvoie403() throws Exception {
        mockMvc.perform(get("/admin/books/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    // Corps VALIDES : @Valid est évalué avant @PreAuthorize, un corps
    // incomplet renverrait 400 et ne testerait pas le contrôle de rôle.
    @Test
    void avecTokenAdherent_postBook_renvoie403() throws Exception {
        mockMvc.perform(post("/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookName\":\"Nouveau\",\"bookAuthor\":\"Auteur\",\"noOfCopies\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void avecTokenAdherent_putBook_renvoie403() throws Exception {
        mockMvc.perform(put("/admin/books/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookName\":\"Modifié\",\"bookAuthor\":\"Auteur\",\"noOfCopies\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void avecTokenAdherent_deleteBook_renvoie403() throws Exception {
        mockMvc.perform(delete("/admin/books/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // L'Admin : CRUD complet
    // ------------------------------------------------------------------
    @Test
    void avecTokenAdmin_getBookById_renvoie200() throws Exception {
        mockMvc.perform(get("/admin/books/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(101))
                .andExpect(jsonPath("$.bookAuthor").value("Auteur 1"));
    }

    @Test
    void avecTokenAdmin_postBook_creeLeLivre() throws Exception {
        mockMvc.perform(post("/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookName\":\"Nouveau Livre\",\"bookAuthor\":\"Auteur\",\"noOfCopies\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Nouveau Livre"))
                .andExpect(jsonPath("$.noOfCopies").value(3));
    }

    @Test
    void avecTokenAdmin_postBookSansAuteur_renvoie400() throws Exception {
        mockMvc.perform(post("/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookName\":\"Nouveau Livre\",\"noOfCopies\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.bookAuthor").exists());
    }

    @Test
    void avecTokenAdmin_postBookJsonInvalide_renvoie400() throws Exception {
        mockMvc.perform(post("/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{pas du json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void avecTokenAdmin_putBook_metAjourLesChamps() throws Exception {
        mockMvc.perform(put("/admin/books/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookName\":\"Titre Modifié\",\"bookAuthor\":\"Autre Auteur\","
                                + "\"bookGenre\":\"SF\",\"noOfCopies\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Titre Modifié"))
                .andExpect(jsonPath("$.bookAuthor").value("Autre Auteur"))
                .andExpect(jsonPath("$.noOfCopies").value(7));
    }

    @Test
    void avecTokenAdmin_deleteBook_renvoieDeletedTrue() throws Exception {
        mockMvc.perform(delete("/admin/books/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void avecTokenAdmin_getBookInconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/admin/books/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------

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
