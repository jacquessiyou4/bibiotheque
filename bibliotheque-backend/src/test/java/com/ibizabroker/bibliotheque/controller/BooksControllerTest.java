package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.BookResponse;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.service.BooksService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de BooksController : BooksService simulé, aucun contexte
 * Spring. Vérifie la pagination transmise au service et la conversion en DTO.
 * La sécurité par rôle est couverte par BooksAdminApiIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class BooksControllerTest {

    private static final Authentication ADMIN = new TestingAuthenticationToken("admin", null, "ROLE_Admin");

    @Mock
    private BooksService booksService;

    @InjectMocks
    private BooksController controller;

    @Test
    void getAllBooks_transmetPaginationEtTriPuisConvertitEnDto() {
        when(booksService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(livre(1, "L1", 3))));

        Page<BookResponse> page = controller.getAllBooks(2, 5, "bookName");

        ArgumentCaptor<Pageable> pagination = ArgumentCaptor.forClass(Pageable.class);
        verify(booksService).findAll(pagination.capture());
        assertThat(pagination.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pagination.getValue().getPageSize()).isEqualTo(5);
        assertThat(pagination.getValue().getSort().getOrderFor("bookName")).isNotNull();
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getBookName()).isEqualTo("L1");
        assertThat(page.getContent().get(0).getNoOfCopies()).isEqualTo(3);
    }

    @Test
    void getBookById_renvoie200AvecLeDto() {
        when(booksService.findById(1)).thenReturn(livre(1, "L1", 3));

        ResponseEntity<BookResponse> reponse = controller.getBookById(1);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().getBookId()).isEqualTo(1);
        assertThat(reponse.getBody().getBookAuthor()).isEqualTo("Auteur");
    }

    @Test
    void createBook_creeLeLivreEtRenvoieLeDto() {
        Books demande = livre(0, "Nouveau", 4);
        when(booksService.create(demande)).thenReturn(livre(9, "Nouveau", 4));

        BookResponse cree = controller.createBook(ADMIN, demande);

        assertThat(cree.getBookId()).isEqualTo(9);
        assertThat(cree.getBookName()).isEqualTo("Nouveau");
    }

    @Test
    void createBook_sansAuthentification_journaliseAnonymeSansErreur() {
        Books demande = livre(0, "Nouveau", 1);
        when(booksService.create(demande)).thenReturn(livre(10, "Nouveau", 1));

        assertThat(controller.createBook(null, demande).getBookId()).isEqualTo(10);
    }

    @Test
    void updateBook_renvoieLeLivreModifie() {
        Books modification = livre(1, "L1 bis", 7);
        when(booksService.update(1, modification)).thenReturn(modification);

        ResponseEntity<BookResponse> reponse = controller.updateBook(ADMIN, 1, modification);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().getBookName()).isEqualTo("L1 bis");
        assertThat(reponse.getBody().getNoOfCopies()).isEqualTo(7);
    }

    @Test
    void deleteBook_supprimeEtRenvoieDeletedTrue() {
        ResponseEntity<Map<String, Boolean>> reponse = controller.deleteBook(ADMIN, 4);

        verify(booksService).delete(4);
        assertThat(reponse.getBody()).containsEntry("deleted", Boolean.TRUE);
    }

    private Books livre(int id, String nom, int exemplaires) {
        Books livre = new Books();
        livre.setBookId(id);
        livre.setBookName(nom);
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(exemplaires);
        return livre;
    }
}
