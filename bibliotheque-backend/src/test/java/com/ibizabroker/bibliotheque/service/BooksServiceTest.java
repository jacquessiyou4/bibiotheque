package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooksServiceTest {

    @Mock
    private BooksRepository booksRepository;

    @InjectMocks
    private BooksService booksService;

    @Test
    void findAll_renvoieLaPageDeLivres() {
        Books livre = unLivre();
        Page<Books> page = new PageImpl<>(Collections.singletonList(livre));
        when(booksRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(booksRepository.count()).thenReturn(1L);

        Page<Books> resultat = booksService.findAll(PageRequest.of(0, 20));

        assertThat(resultat.getContent()).hasSize(1);
        assertThat(resultat.getContent().get(0).getBookName()).isEqualTo("Le Petit Prince");
    }

    @Test
    void findById_livreExistant_renvoieLeLivre() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(unLivre()));

        Books resultat = booksService.findById(1);

        assertThat(resultat.getBookName()).isEqualTo("Le Petit Prince");
    }

    @Test
    void findById_livreIntrouvable_lanceNotFound() {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> booksService.findById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre avec id 999");
    }

    @Test
    void create_sauveLeLivre() {
        Books livre = unLivre();
        when(booksRepository.save(any(Books.class))).thenReturn(livre);

        Books resultat = booksService.create(livre);

        assertThat(resultat).isNotNull();
        verify(booksRepository).save(livre);
    }

    @Test
    void update_livreExistant_metsAJourLesChamps() {
        Books existant = unLivre();
        when(booksRepository.findById(1)).thenReturn(Optional.of(existant));
        when(booksRepository.save(any(Books.class))).thenReturn(existant);

        Books details = new Books();
        details.setBookName("Nouveau Nom");
        details.setBookAuthor("Nouvel Auteur");
        details.setBookGenre("Science-Fiction");
        details.setNoOfCopies(5);

        Books resultat = booksService.update(1, details);

        assertThat(resultat.getBookName()).isEqualTo("Nouveau Nom");
        assertThat(resultat.getBookAuthor()).isEqualTo("Nouvel Auteur");
    }

    @Test
    void update_livreIntrouvable_lanceNotFound() {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> booksService.update(999, unLivre()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_livreExistant_supprimeLeLivre() {
        Books livre = unLivre();
        when(booksRepository.findById(1)).thenReturn(Optional.of(livre));

        booksService.delete(1);

        verify(booksRepository).delete(livre);
    }

    @Test
    void delete_livreIntrouvable_lanceNotFound() {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> booksService.delete(999))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void count_renvoieLeNombreDeLivres() {
        when(booksRepository.count()).thenReturn(42L);

        assertThat(booksService.count()).isEqualTo(42L);
    }

    private Books unLivre() {
        Books livre = new Books();
        livre.setBookId(1);
        livre.setBookName("Le Petit Prince");
        livre.setBookAuthor("Antoine de Saint-Exupéry");
        livre.setBookGenre("Conte");
        livre.setNoOfCopies(3);
        return livre;
    }
}
