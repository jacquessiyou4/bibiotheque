package com.ibizabroker.bibliotheque.emprunts.web;

import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import com.ibizabroker.bibliotheque.catalogue.internal.Books;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Tests d'intégration des emprunts (endpoints /borrow). Les endpoints exigent
 * un jeton (décodage JWT simulé), les données viennent de repositories
 * simulés. On vérifie les règles de gestion : décrément du stock à
 * l'emprunt, refus hors stock, dates issueDate/dueDate (+ 7 jours),
 * ré-incrément du stock et returnDate au retour, refus d'un double retour,
 * dates sérialisées en ISO-8601.
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
class BorrowApiIntegrationTest {

    private static final String BEARER_ADHERENT = "Bearer token-adherent";
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

    private Books livre;
    private Users adherent;

    @BeforeEach
    void setUp() {
        livre = new Books();
        livre.setBookId(3);
        livre.setBookName("L2");
        livre.setBookAuthor("Auteur 2");
        livre.setBookGenre("Essai");
        livre.setNoOfCopies(2);

        adherent = new Users();
        adherent.setUserId(1);
        adherent.setUsername("A1");
        adherent.setName("Adherent Un");

        when(booksRepository.findById(3)).thenReturn(Optional.of(livre));
        when(usersRepository.findById(1)).thenReturn(Optional.of(adherent));
        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(adherent));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Requêtes atomiques simulées : le stock du livre de test fait foi.
        when(booksRepository.decrementerStock(3)).thenAnswer(invocation -> livre.getNoOfCopies() > 0 ? 1 : 0);
        when(booksRepository.incrementerStock(3)).thenReturn(1);

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
    void sansToken_postBorrow_renvoie401() throws Exception {
        mockMvc.perform(post("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isUnauthorized());

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    // ------------------------------------------------------------------
    // Emprunt : décrément du stock + dates issueDate / dueDate (+ 7 jours)
    // ------------------------------------------------------------------
    @Test
    void postBorrow_decrementeLeStockEtPositionneLesDates() throws Exception {
        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(3))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.returnDate").doesNotExist())
                .andExpect(jsonPath("$.issueDate").value(
                        org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*")));

        verify(booksRepository).decrementerStock(3);

        ArgumentCaptor<Borrow> empruntsCapturés = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowRepository).save(empruntsCapturés.capture());
        Borrow emprunt = empruntsCapturés.getValue();
        assertThat(emprunt.getIssueDate()).isNotNull();
        assertThat(Duration.between(emprunt.getIssueDate(), emprunt.getDueDate()).toDays()).isEqualTo(7);
    }

    @Test
    void postBorrow_stockEpuise_renvoie400EtNeSauvegardeRien() throws Exception {
        livre.setNoOfCopies(0);

        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("n'est plus disponible")));

        verify(booksRepository, never()).incrementerStock(any());
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    // ------------------------------------------------------------------
    // Retour : ré-incrément du stock + returnDate positionnée
    // ------------------------------------------------------------------
    @Test
    void putBorrow_restitueLaCopieEtPositionneLaDateDeRetour() throws Exception {
        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(1);
        emprunt.setUserId(1);
        emprunt.setBookId(3);
        emprunt.setIssueDate(LocalDateTime.now().minusDays(2));

        when(borrowRepository.findById(1)).thenReturn(Optional.of(emprunt));

        mockMvc.perform(put("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":1}"))
                .andExpect(status().isOk())
                // ISO-8601 : relisible par new Date() dans le navigateur.
                .andExpect(jsonPath("$.returnDate").value(
                        org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*")));

        verify(booksRepository).incrementerStock(3);

        ArgumentCaptor<Borrow> empruntsCapturés = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowRepository).save(empruntsCapturés.capture());
        assertThat(empruntsCapturés.getValue().getReturnDate()).isNotNull();
    }

    @Test
    void putBorrow_empruntDejaRendu_renvoie400EtNeModifiePasLeStock() throws Exception {
        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(1);
        emprunt.setUserId(1);
        emprunt.setBookId(3);
        emprunt.setIssueDate(LocalDateTime.now().minusDays(2));
        emprunt.setReturnDate(LocalDateTime.now());

        when(borrowRepository.findById(1)).thenReturn(Optional.of(emprunt));

        mockMvc.perform(put("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":1}"))
                .andExpect(status().isBadRequest());

        verify(booksRepository, never()).decrementerStock(any());
        verify(booksRepository, never()).incrementerStock(any());
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    // ------------------------------------------------------------------
    // Consultations
    // ------------------------------------------------------------------
    @Test
    void getBorrows_admin_renvoieTousLesEmprunts() throws Exception {
        when(borrowRepository.findAll()).thenReturn(Collections.singletonList(emprunt(1)));

        mockMvc.perform(get("/borrow").header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    // ------------------------------------------------------------------
    // Propriété : un adhérent n'agit que sur ses propres emprunts (403)
    // ------------------------------------------------------------------
    @Test
    void getBorrows_adherent_renvoie403() throws Exception {
        mockMvc.perform(get("/borrow").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void postBorrow_adherentPourUnAutreUtilisateur_renvoie403EtNeSauvegardeRien() throws Exception {
        mockMvc.perform(post("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"bookId\":3}"))
                .andExpect(status().isForbidden());

        verify(booksRepository, never()).decrementerStock(any());
        verify(booksRepository, never()).incrementerStock(any());
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void putBorrow_adherentSurLEmpruntDUnAutre_renvoie403EtNeModifiePasLeStock() throws Exception {
        Borrow empruntDAutrui = emprunt(5);
        empruntDAutrui.setUserId(2);
        when(borrowRepository.findById(5)).thenReturn(Optional.of(empruntDAutrui));

        mockMvc.perform(put("/borrow")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":5}"))
                .andExpect(status().isForbidden());

        verify(booksRepository, never()).decrementerStock(any());
        verify(booksRepository, never()).incrementerStock(any());
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void getBorrowsByUser_adherentSurUnAutreUtilisateur_renvoie403() throws Exception {
        mockMvc.perform(get("/borrow/user/2").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBorrowsByBook_adherent_renvoie403() throws Exception {
        mockMvc.perform(get("/borrow/book/3").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBorrowsByUser_renvoieLesEmpruntsDeLUtilisateur() throws Exception {
        when(borrowRepository.findByUserId(1)).thenReturn(Arrays.asList(emprunt(1), emprunt(2)));

        mockMvc.perform(get("/borrow/user/1").header(HttpHeaders.AUTHORIZATION, BEARER_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void getBorrowsByBook_renvoieLesEmpruntsDuLivre() throws Exception {
        when(borrowRepository.findByBookId(3)).thenReturn(Collections.singletonList(emprunt(1)));

        mockMvc.perform(get("/borrow/book/3").header(HttpHeaders.AUTHORIZATION, BEARER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    // ------------------------------------------------------------------

    private Borrow emprunt(int borrowId) {
        Borrow borrow = new Borrow();
        borrow.setBorrowId(borrowId);
        borrow.setUserId(1);
        borrow.setBookId(3);
        return borrow;
    }
}
