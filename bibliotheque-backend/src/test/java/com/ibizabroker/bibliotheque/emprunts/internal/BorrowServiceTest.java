package com.ibizabroker.bibliotheque.emprunts.internal;

import com.ibizabroker.bibliotheque.shared.config.ReglesBibliotheque;
import com.ibizabroker.bibliotheque.shared.observabilite.MetriquesMetier;

import com.ibizabroker.bibliotheque.catalogue.api.CatalogueApi;
import com.ibizabroker.bibliotheque.catalogue.api.LivreResume;
import com.ibizabroker.bibliotheque.emprunts.api.BorrowResponse;
import com.ibizabroker.bibliotheque.shared.error.BadRequestException;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateurResume;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** BorrowService ne connaît le catalogue et les comptes que par leurs API. */
@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private CatalogueApi catalogueApi;

    @Mock
    private UtilisateursApi utilisateursApi;

    // Règles par défaut (7 jours, 3 réservations actives), comme en production.
    @Spy
    private ReglesBibliotheque regles = new ReglesBibliotheque();

    @Mock
    private MetriquesMetier metriques;

    @InjectMocks
    private BorrowService borrowService;

    @Test
    void borrowBook_livreDisponible_creeLEmpruntAvecSesDates() {
        when(utilisateursApi.utilisateur(1)).thenReturn(Optional.of(unUtilisateur()));
        when(catalogueApi.livre(1)).thenReturn(Optional.of(unLivre(3)));
        when(catalogueApi.retirerExemplaire(1)).thenReturn(true);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow emprunt = borrowService.borrowBook(1, 1);

        verify(catalogueApi).retirerExemplaire(1);
        verify(metriques).empruntEnregistre();
        assertThat(emprunt.getBookId()).isEqualTo(1);
        assertThat(emprunt.getUserId()).isEqualTo(1);
        assertThat(emprunt.getReturnDate()).isNull();
        assertThat(Duration.between(emprunt.getIssueDate(), emprunt.getDueDate()).toDays()).isEqualTo(7);
    }

    @Test
    void borrowBook_livreIndisponible_lanceBadRequest() {
        when(utilisateursApi.utilisateur(1)).thenReturn(Optional.of(unUtilisateur()));
        when(catalogueApi.livre(1)).thenReturn(Optional.of(unLivre(0)));
        // Stock épuisé (ou pris par un emprunt simultané) : aucun exemplaire retiré.
        when(catalogueApi.retirerExemplaire(1)).thenReturn(false);

        assertThatThrownBy(() -> borrowService.borrowBook(1, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est plus disponible");
        verify(borrowRepository, never()).save(any(Borrow.class));
        verify(metriques, never()).empruntEnregistre();
    }

    @Test
    void borrowBook_dureeDEmpruntConfigurable() {
        regles.setDureeEmpruntJours(21);
        when(utilisateursApi.utilisateur(1)).thenReturn(Optional.of(unUtilisateur()));
        when(catalogueApi.livre(1)).thenReturn(Optional.of(unLivre(3)));
        when(catalogueApi.retirerExemplaire(1)).thenReturn(true);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow emprunt = borrowService.borrowBook(1, 1);

        assertThat(Duration.between(emprunt.getIssueDate(), emprunt.getDueDate()).toDays()).isEqualTo(21);
    }

    @Test
    void borrowBook_utilisateurIntrouvable_lanceNotFound() {
        when(utilisateursApi.utilisateur(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.borrowBook(1, 999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");
        verify(catalogueApi, never()).retirerExemplaire(any());
    }

    @Test
    void borrowBook_livreIntrouvable_lanceNotFound() {
        when(utilisateursApi.utilisateur(1)).thenReturn(Optional.of(unUtilisateur()));
        when(catalogueApi.livre(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.borrowBook(999, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre introuvable");
    }

    @Test
    void returnBook_empruntExistant_rendLeLivre() {
        when(borrowRepository.findById(1)).thenReturn(Optional.of(unEmprunt()));
        when(catalogueApi.livre(1)).thenReturn(Optional.of(unLivre(2)));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow resultat = borrowService.returnBook(1, null);

        assertThat(resultat.getReturnDate()).isNotNull();
        verify(catalogueApi).remettreExemplaire(1);
        verify(metriques).retourEnregistre();
    }

    @Test
    void returnBook_proprietaireDifferent_lanceForbiddenSansToucherAuStock() {
        when(borrowRepository.findById(1)).thenReturn(Optional.of(unEmprunt()));

        assertThatThrownBy(() -> borrowService.returnBook(1, 2))
                .isInstanceOf(ForbiddenException.class);
        verify(catalogueApi, never()).remettreExemplaire(any());
    }

    @Test
    void returnBook_empruntDejaRendu_lanceBadRequestSansToucherAuStock() {
        Borrow emprunt = unEmprunt();
        emprunt.setReturnDate(LocalDateTime.now());
        when(borrowRepository.findById(1)).thenReturn(Optional.of(emprunt));

        assertThatThrownBy(() -> borrowService.returnBook(1, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà été rendu");
        // Le contrôle a lieu avant tout accès au livre : stock inchangé.
        verify(catalogueApi, never()).livre(any());
        verify(catalogueApi, never()).remettreExemplaire(any());
    }

    @Test
    void returnBook_empruntIntrouvable_lanceNotFound() {
        when(borrowRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.returnBook(999, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Emprunt introuvable");
    }

    @Test
    void returnBook_livreIntrouvable_lanceNotFound() {
        when(borrowRepository.findById(1)).thenReturn(Optional.of(unEmprunt()));
        when(catalogueApi.livre(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.returnBook(1, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre introuvable");
    }

    @Test
    void resolveUserId_renvoieLIdentifiantDuCompteLocal() {
        when(utilisateursApi.parUsername("a1")).thenReturn(Optional.of(unUtilisateur()));

        assertThat(borrowService.resolveUserId("a1")).isEqualTo(1);
    }

    @Test
    void resolveUserId_compteLocalAbsent_lanceForbidden() {
        when(utilisateursApi.parUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.resolveUserId("ghost"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAll_renvoieTousLesEmprunts() {
        when(borrowRepository.findAll()).thenReturn(Arrays.asList(unEmprunt(), unEmprunt()));

        assertThat(borrowService.findAll()).hasSize(2);
    }

    @Test
    void findByBookId_renvoieLesEmpruntsDuLivre() {
        when(borrowRepository.findByBookId(1)).thenReturn(Collections.singletonList(unEmprunt()));

        assertThat(borrowService.findByBookId(1)).hasSize(1);
    }

    @Test
    void empruntsDe_convertitLesEmpruntsDeLUtilisateurEnDto() {
        when(borrowRepository.findByUserId(1)).thenReturn(Collections.singletonList(unEmprunt()));

        List<BorrowResponse> emprunts = borrowService.empruntsDe(1);

        assertThat(emprunts).extracting(BorrowResponse::getBorrowId).containsExactly(1);
    }

    private UtilisateurResume unUtilisateur() {
        return new UtilisateurResume(1, "testuser", "Utilisateur Test");
    }

    private LivreResume unLivre(int copies) {
        return new LivreResume(1, "Le Petit Prince", copies);
    }

    private Borrow unEmprunt() {
        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(1);
        emprunt.setBookId(1);
        emprunt.setUserId(1);
        return emprunt;
    }
}
