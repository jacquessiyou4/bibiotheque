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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
 * Tests d'intégration des emprunts (endpoints /borrow). Les endpoints sont
 * accessibles sans authentification (permitAll), les données viennent de
 * repositories simulés. On vérifie les règles de gestion : décrément du
 * stock à l'emprunt, refus hors stock, dates issueDate/dueDate (+ 7 jours),
 * ré-incrément du stock et returnDate au retour, sérialisation dd-MM-yyyy.
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
        when(booksRepository.save(any(Books.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------
    // Emprunt : décrément du stock + dates issueDate / dueDate (+ 7 jours)
    // ------------------------------------------------------------------
    @Test
    void postBorrow_decrementeLeStockEtPositionneLesDates() throws Exception {
        mockMvc.perform(post("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":10,\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(
                        org.hamcrest.Matchers.containsString("Adherent Un has borrowed one copy of \"L2\"!")));

        ArgumentCaptor<Books> livresCapturés = ArgumentCaptor.forClass(Books.class);
        verify(booksRepository).save(livresCapturés.capture());
        assertThat(livresCapturés.getValue().getNoOfCopies()).isEqualTo(1);

        ArgumentCaptor<Borrow> empruntsCapturés = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowRepository).save(empruntsCapturés.capture());
        Borrow emprunt = empruntsCapturés.getValue();
        assertThat(emprunt.getIssueDate()).isNotNull();
        long ecritureJours = (emprunt.getDueDate().getTime() - emprunt.getIssueDate().getTime()) / (1000 * 60 * 60 * 24);
        assertThat(ecritureJours).isEqualTo(7);
    }

    @Test
    void postBorrow_stockEpuise_renvoieLeMessageHorsStockEtNeSauvegardeRien() throws Exception {
        livre.setNoOfCopies(0);

        mockMvc.perform(post("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":10,\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(
                        org.hamcrest.Matchers.containsString("The book \"L2\" is out of stock!")));

        verify(booksRepository, never()).save(any(Books.class));
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
        emprunt.setIssueDate(new Date());

        when(borrowRepository.findById(1)).thenReturn(Optional.of(emprunt));

        mockMvc.perform(put("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":1}"))
                .andExpect(status().isOk())
                // Le retour est sérialisé au format dd-MM-yyyy (JsonDataSerializer).
                .andExpect(jsonPath("$.returnDate").value(
                        org.hamcrest.Matchers.matchesPattern("\\d{2}-\\d{2}-\\d{4}")));

        ArgumentCaptor<Books> livresCapturés = ArgumentCaptor.forClass(Books.class);
        verify(booksRepository).save(livresCapturés.capture());
        assertThat(livresCapturés.getValue().getNoOfCopies()).isEqualTo(3);

        ArgumentCaptor<Borrow> empruntsCapturés = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowRepository).save(empruntsCapturés.capture());
        assertThat(empruntsCapturés.getValue().getReturnDate()).isNotNull();
    }

    // ------------------------------------------------------------------
    // Consultations
    // ------------------------------------------------------------------
    @Test
    void getBorrows_renvoieTousLesEmprunts() throws Exception {
        when(borrowRepository.findAll()).thenReturn(Collections.singletonList(emprunt(1)));

        mockMvc.perform(get("/borrow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void getBorrowsByUser_renvoieLesEmpruntsDeLUtilisateur() throws Exception {
        when(borrowRepository.findByUserId(1)).thenReturn(Arrays.asList(emprunt(1), emprunt(2)));

        mockMvc.perform(get("/borrow/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void getBorrowsByBook_renvoieLesEmpruntsDuLivre() throws Exception {
        when(borrowRepository.findByBookId(3)).thenReturn(Collections.singletonList(emprunt(1)));

        mockMvc.perform(get("/borrow/book/3"))
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
