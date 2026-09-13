package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BadRequestException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private BooksRepository booksRepository;

    @InjectMocks
    private BorrowService borrowService;

    @Test
    void borrowBook_livreDisponible_empruntReussi() {
        Users user = unUtilisateur();
        Books livre = unLivre(3);
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(booksRepository.findById(1)).thenReturn(Optional.of(livre));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow emprunt = new Borrow();
        emprunt.setBookId(1);
        emprunt.setUserId(1);

        String resultat = borrowService.borrowBook(emprunt);

        assertThat(resultat).contains("a emprunté une copie");
        assertThat(livre.getNoOfCopies()).isEqualTo(2);
        verify(booksRepository).save(livre);
    }

    @Test
    void borrowBook_livreIndisponible_lanceBadRequest() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(unUtilisateur()));
        when(booksRepository.findById(1)).thenReturn(Optional.of(unLivre(0)));

        Borrow emprunt = new Borrow();
        emprunt.setBookId(1);
        emprunt.setUserId(1);

        assertThatThrownBy(() -> borrowService.borrowBook(emprunt))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est plus disponible");
    }

    @Test
    void borrowBook_utilisateurIntrouvable_lanceNotFound() {
        when(usersRepository.findById(999)).thenReturn(Optional.empty());

        Borrow emprunt = new Borrow();
        emprunt.setBookId(1);
        emprunt.setUserId(999);

        assertThatThrownBy(() -> borrowService.borrowBook(emprunt))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void borrowBook_livreIntrouvable_lanceNotFound() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(unUtilisateur()));
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        Borrow emprunt = new Borrow();
        emprunt.setBookId(999);
        emprunt.setUserId(1);

        assertThatThrownBy(() -> borrowService.borrowBook(emprunt))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre introuvable");
    }

    @Test
    void returnBook_empruntExistant_rendLeLivre() {
        Borrow emprunt = unEmprunt();
        Books livre = unLivre(2);
        when(borrowRepository.findById(1)).thenReturn(Optional.of(emprunt));
        when(booksRepository.findById(1)).thenReturn(Optional.of(livre));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow resultat = borrowService.returnBook(emprunt);

        assertThat(resultat.getReturnDate()).isNotNull();
        assertThat(livre.getNoOfCopies()).isEqualTo(3);
        verify(booksRepository).save(livre);
    }

    @Test
    void returnBook_empruntIntrouvable_lanceNotFound() {
        when(borrowRepository.findById(999)).thenReturn(Optional.empty());

        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(999);

        assertThatThrownBy(() -> borrowService.returnBook(emprunt))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Emprunt introuvable");
    }

    @Test
    void returnBook_livreIntrouvable_lanceNotFound() {
        Borrow emprunt = unEmprunt();
        when(borrowRepository.findById(1)).thenReturn(Optional.of(emprunt));
        when(booksRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.returnBook(emprunt))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre introuvable");
    }

    @Test
    void findAll_renvoieTousLesEmprunts() {
        when(borrowRepository.findAll()).thenReturn(Arrays.asList(unEmprunt(), unEmprunt2()));

        assertThat(borrowService.findAll()).hasSize(2);
    }

    @Test
    void findByUserId_renvoieLesEmpruntsDeLUtilisateur() {
        when(borrowRepository.findByUserId(1)).thenReturn(Collections.singletonList(unEmprunt()));

        assertThat(borrowService.findByUserId(1)).hasSize(1);
    }

    @Test
    void findByBookId_renvoieLesEmpruntsDuLivre() {
        when(borrowRepository.findByBookId(1)).thenReturn(Collections.singletonList(unEmprunt()));

        assertThat(borrowService.findByBookId(1)).hasSize(1);
    }

    private Users unUtilisateur() {
        Users user = new Users();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setName("Utilisateur Test");
        return user;
    }

    private Books unLivre(int copies) {
        Books livre = new Books();
        livre.setBookId(1);
        livre.setBookName("Le Petit Prince");
        livre.setBookAuthor("Antoine de Saint-Exupéry");
        livre.setNoOfCopies(copies);
        return livre;
    }

    private Borrow unEmprunt() {
        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(1);
        emprunt.setBookId(1);
        emprunt.setUserId(1);
        return emprunt;
    }

    private Borrow unEmprunt2() {
        Borrow emprunt = new Borrow();
        emprunt.setBorrowId(2);
        emprunt.setBookId(2);
        emprunt.setUserId(1);
        return emprunt;
    }
}
