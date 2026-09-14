package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.BorrowResponse;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.service.BorrowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de BorrowController : BorrowService simulé, aucun contexte
 * Spring. Vérifie la règle « un User n'agit que sur ses emprunts » et la
 * conversion des entités en BorrowResponse. Les rôles exigés par
 * @PreAuthorize sont couverts par BorrowApiIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class BorrowControllerTest {

    private static final Authentication ADHERENT_A1 =
            new TestingAuthenticationToken("a1", null, "ROLE_User", "ROLE_ADHERENT");
    private static final Authentication BIBLIOTHECAIRE =
            new TestingAuthenticationToken("admin", null, "ROLE_Admin", "ROLE_BIBLIOTHECAIRE");

    @Mock
    private BorrowService borrowService;

    @InjectMocks
    private BorrowController controller;

    @Test
    void borrowBook_adherentPourLuiMeme_empruntLeLivre() {
        Borrow demande = emprunt(null, 1, 2);
        when(borrowService.resolveUserId("a1")).thenReturn(2);
        when(borrowService.borrowBook(demande)).thenReturn("Livre emprunté");

        assertThat(controller.borrowBook(ADHERENT_A1, demande)).isEqualTo("Livre emprunté");
    }

    @Test
    void borrowBook_adherentPourUnAutreCompte_lanceForbiddenSansEmprunter() {
        when(borrowService.resolveUserId("a1")).thenReturn(2);

        assertThatThrownBy(() -> controller.borrowBook(ADHERENT_A1, emprunt(null, 1, 3)))
                .isInstanceOf(ForbiddenException.class);

        verify(borrowService, never()).borrowBook(any(Borrow.class));
    }

    @Test
    void borrowBook_bibliothecaire_peutEmprunterPourUnAdherent() {
        Borrow demande = emprunt(null, 1, 3);
        when(borrowService.borrowBook(demande)).thenReturn("Livre emprunté");

        controller.borrowBook(BIBLIOTHECAIRE, demande);

        verify(borrowService, never()).resolveUserId(anyString());
    }

    @Test
    void getAllBorrow_convertitChaqueEmpruntEnDto() {
        Borrow emprunt = emprunt(5, 1, 2);
        when(borrowService.findAll()).thenReturn(Collections.singletonList(emprunt));

        List<BorrowResponse> emprunts = controller.getAllBorrow();

        assertThat(emprunts).hasSize(1);
        assertThat(emprunts.get(0).getBorrowId()).isEqualTo(5);
        assertThat(emprunts.get(0).getBookId()).isEqualTo(1);
        assertThat(emprunts.get(0).getUserId()).isEqualTo(2);
        assertThat(emprunts.get(0).getIssueDate()).isEqualTo(emprunt.getIssueDate());
        assertThat(emprunts.get(0).getDueDate()).isEqualTo(emprunt.getDueDate());
        assertThat(emprunts.get(0).getReturnDate()).isNull();
    }

    @Test
    void returnBook_adherent_imposeSonPropreIdCommeProprietaire() {
        Borrow demande = emprunt(5, null, null);
        Borrow rendu = emprunt(5, 1, 2);
        rendu.setReturnDate(new Date());
        when(borrowService.resolveUserId("a1")).thenReturn(2);
        when(borrowService.returnBook(demande, 2)).thenReturn(rendu);

        BorrowResponse reponse = controller.returnBook(ADHERENT_A1, demande);

        assertThat(reponse.getBorrowId()).isEqualTo(5);
        assertThat(reponse.getReturnDate()).isNotNull();
    }

    @Test
    void returnBook_bibliothecaire_nImposeAucunProprietaire() {
        Borrow demande = emprunt(5, null, null);
        when(borrowService.returnBook(eq(demande), isNull())).thenReturn(emprunt(5, 1, 2));

        controller.returnBook(BIBLIOTHECAIRE, demande);

        verify(borrowService).returnBook(eq(demande), isNull());
        verify(borrowService, never()).resolveUserId(anyString());
    }

    @Test
    void booksBorrowedByUser_adherentLesSiens_renvoieLesDto() {
        when(borrowService.resolveUserId("a1")).thenReturn(2);
        when(borrowService.findByUserId(2)).thenReturn(Collections.singletonList(emprunt(5, 1, 2)));

        assertThat(controller.booksBorrowedByUser(ADHERENT_A1, 2)).extracting(BorrowResponse::getBorrowId)
                .containsExactly(5);
    }

    @Test
    void booksBorrowedByUser_adherentCeuxDUnAutre_lanceForbidden() {
        when(borrowService.resolveUserId("a1")).thenReturn(2);

        assertThatThrownBy(() -> controller.booksBorrowedByUser(ADHERENT_A1, 3))
                .isInstanceOf(ForbiddenException.class);

        verify(borrowService, never()).findByUserId(3);
    }

    @Test
    void bookBorrowHistory_renvoieLesDtoDuLivre() {
        when(borrowService.findByBookId(1)).thenReturn(Collections.singletonList(emprunt(5, 1, 2)));

        assertThat(controller.bookBorrowHistory(1)).extracting(BorrowResponse::getBookId).containsExactly(1);
    }

    private Borrow emprunt(Integer borrowId, Integer bookId, Integer userId) {
        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(borrowId);
        emprunt.setBookId(bookId);
        emprunt.setUserId(userId);
        emprunt.setIssueDate(new Date(0));
        emprunt.setDueDate(new Date(86_400_000L));
        return emprunt;
    }
}
