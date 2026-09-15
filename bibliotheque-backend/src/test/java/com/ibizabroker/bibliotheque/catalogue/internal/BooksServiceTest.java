package com.ibizabroker.bibliotheque.catalogue.internal;

import com.ibizabroker.bibliotheque.catalogue.api.LivreResume;

import com.ibizabroker.bibliotheque.catalogue.web.BookRequest;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
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
import java.util.Map;
import java.util.Arrays;
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
    void create_copieLaDemandeDansUnNouveauLivre() {
        when(booksRepository.save(any(Books.class))).thenAnswer(inv -> inv.getArgument(0));

        Books resultat = booksService.create(new BookRequest("Le Petit Prince", "Saint-Exupéry", "Conte", 3));

        assertThat(resultat.getBookId()).isNull();
        assertThat(resultat.getBookName()).isEqualTo("Le Petit Prince");
        assertThat(resultat.getBookAuthor()).isEqualTo("Saint-Exupéry");
        assertThat(resultat.getBookGenre()).isEqualTo("Conte");
        assertThat(resultat.getNoOfCopies()).isEqualTo(3);
    }

    @Test
    void update_livreExistant_metsAJourLesChampsEtGardeLId() {
        Books existant = unLivre();
        when(booksRepository.findById(1)).thenReturn(Optional.of(existant));
        when(booksRepository.save(any(Books.class))).thenAnswer(inv -> inv.getArgument(0));

        Books resultat = booksService.update(1,
                new BookRequest("Nouveau Nom", "Nouvel Auteur", "Science-Fiction", 5));

        assertThat(resultat.getBookId()).isEqualTo(1);
        assertThat(resultat.getBookName()).isEqualTo("Nouveau Nom");
        assertThat(resultat.getBookAuthor()).isEqualTo("Nouvel Auteur");
        assertThat(resultat.getBookGenre()).isEqualTo("Science-Fiction");
        assertThat(resultat.getNoOfCopies()).isEqualTo(5);
    }

    @Test
    void update_livreIntrouvable_lanceNotFound() {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> booksService.update(999, new BookRequest("T", "A", null, 1)))
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


    // ------------------------------------------------------------------
    // CatalogueApi
    // ------------------------------------------------------------------
    @Test
    void livre_renvoieLeResumeDuLivre() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(unLivre()));

        LivreResume resume = booksService.livre(1).orElseThrow(AssertionError::new);

        assertThat(resume.getBookName()).isEqualTo("Le Petit Prince");
        assertThat(resume.estDisponible()).isTrue();
    }

    @Test
    void livres_chargeLesLivresEnUneRequete() {
        when(booksRepository.findAllById(Arrays.asList(1, 2))).thenReturn(Collections.singletonList(unLivre()));

        Map<Integer, LivreResume> livres = booksService.livres(Arrays.asList(1, 2));

        assertThat(livres).containsOnlyKeys(1);
    }

    @Test
    void livres_sansIdentifiant_nInterrogePasLaBase() {
        assertThat(booksService.livres(Collections.emptyList())).isEmpty();
        verify(booksRepository, never()).findAllById(any());
    }

    @Test
    void retirerExemplaire_indiqueSiUnExemplaireAEteRetire() {
        when(booksRepository.decrementerStock(1)).thenReturn(1);
        when(booksRepository.decrementerStock(2)).thenReturn(0);

        assertThat(booksService.retirerExemplaire(1)).isTrue();
        assertThat(booksService.retirerExemplaire(2)).isFalse();
    }

    @Test
    void remettreExemplaire_incrementeLeStock() {
        booksService.remettreExemplaire(1);

        verify(booksRepository).incrementerStock(1);
    }

    @Test
    void livreSansExemplaire_nEstPasDisponible() {
        assertThat(new LivreResume(3, "Épuisé", 0).estDisponible()).isFalse();
        assertThat(new LivreResume(3, "Inconnu", null).estDisponible()).isFalse();
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
