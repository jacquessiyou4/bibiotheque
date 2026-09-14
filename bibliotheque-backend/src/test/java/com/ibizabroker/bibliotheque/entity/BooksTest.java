package com.ibizabroker.bibliotheque.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de la règle de gestion de stock portée par l'entité Books :
 * un emprunt décrémente le nombre de copies, un retour le ré-incrémente.
 */
class BooksTest {

    @Test
    void borrowBook_decrementeLeNombreDeCopies() {
        Books livre = livre(3);

        livre.borrowBook();

        assertThat(livre.getNoOfCopies()).isEqualTo(2);
    }

    @Test
    void returnBook_incrementeLeNombreDeCopies() {
        Books livre = livre(0);

        livre.returnBook();

        assertThat(livre.getNoOfCopies()).isEqualTo(1);
    }

    @Test
    void unAllerRetourCompletRedonneLeStockInitial() {
        Books livre = livre(5);

        livre.borrowBook();
        livre.borrowBook();
        livre.returnBook();

        assertThat(livre.getNoOfCopies()).isEqualTo(4);
    }

    @Test
    void borrowBook_decrementeMemeSiCopiesEstZero() {
        Books livre = livre(0);

        livre.borrowBook();

        assertThat(livre.getNoOfCopies()).isEqualTo(-1);
    }

    @Test
    void empruntsMultiples_renvoientLeBonCompteur() {
        Books livre = livre(3);

        livre.borrowBook();
        livre.borrowBook();
        livre.borrowBook();

        assertThat(livre.getNoOfCopies()).isEqualTo(0);
    }

    @Test
    void retoursMultiples_renvoientLeBonCompteur() {
        Books livre = livre(0);

        livre.returnBook();
        livre.returnBook();
        livre.returnBook();

        assertThat(livre.getNoOfCopies()).isEqualTo(3);
    }

    @Test
    void gettersSetters_fonctionnentCorrectement() {
        Books livre = new Books();
        livre.setBookId(42);
        livre.setBookName("Test Book");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(10);

        assertThat(livre.getBookId()).isEqualTo(42);
        assertThat(livre.getBookName()).isEqualTo("Test Book");
        assertThat(livre.getBookAuthor()).isEqualTo("Auteur");
        assertThat(livre.getBookGenre()).isEqualTo("Roman");
        assertThat(livre.getNoOfCopies()).isEqualTo(10);
    }

    private Books livre(int copies) {
        Books livre = new Books();
        livre.setBookId(101);
        livre.setBookName("Livre Test");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(copies);
        return livre;
    }
}
