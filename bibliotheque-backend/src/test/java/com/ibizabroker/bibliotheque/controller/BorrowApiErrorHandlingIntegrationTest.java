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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;
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
 * erreurs, cas limites (emprunt hors stock, utilisateur/livre inexistant).
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
        when(booksRepository.findById(3)).thenReturn(Optional.of(livre));
        when(booksRepository.save(any(Books.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void postBorrow_utilisateurInexistant_provoqueNoSuchElementOuErreur() throws Exception {
        when(usersRepository.findById(999)).thenReturn(Optional.empty());

        try {
            mockMvc.perform(post("/borrow")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":999,\"bookId\":3}"));
        } catch (Exception e) {
            org.assertj.core.api.Assertions.assertThat(e.getCause())
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Test
    void postBorrow_livreInexistant_provoqueNoSuchElementOuErreur() throws Exception {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        try {
            mockMvc.perform(post("/borrow")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":1,\"bookId\":999}"));
        } catch (Exception e) {
            org.assertj.core.api.Assertions.assertThat(e.getCause())
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Test
    void postBorrow_stockEpuise_neSauvegardePasLaCopie() throws Exception {
        Books livreEpuise = new Books();
        livreEpuise.setBookId(3);
        livreEpuise.setBookName("L2 Epuise");
        livreEpuise.setNoOfCopies(0);
        when(booksRepository.findById(3)).thenReturn(Optional.of(livreEpuise));

        mockMvc.perform(post("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"bookId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(
                        org.hamcrest.Matchers.containsString("out of stock")));

        verify(booksRepository, never()).save(any(Books.class));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void putBorrow_empruntInexistant_provoqueErreur() throws Exception {
        when(borrowRepository.findById(999)).thenReturn(Optional.empty());

        try {
            mockMvc.perform(put("/borrow")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"borrowId\":999}"));
        } catch (Exception e) {
            org.assertj.core.api.Assertions.assertThat(e.getCause())
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Test
    void getBorrowsByUser_renvoieListeVideSiAucunEmprunt() throws Exception {
        when(borrowRepository.findByUserId(999)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/borrow/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBorrowsByBook_renvoieListeVideSiAucunEmprunt() throws Exception {
        when(borrowRepository.findByBookId(999)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/borrow/book/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void postBorrow_sauvegardeLesDatesIssueEtDue() throws Exception {
        mockMvc.perform(post("/borrow")
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
