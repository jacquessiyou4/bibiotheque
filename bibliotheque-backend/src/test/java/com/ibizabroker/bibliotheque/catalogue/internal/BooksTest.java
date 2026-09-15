package com.ibizabroker.bibliotheque.catalogue.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de l'entité Books. Le stock ne se modifie plus en mémoire
 * (voir BooksRepository.decrementerStock / incrementerStock, vérifiés contre
 * PostgreSQL par KeycloakEndToEndIT).
 */
class BooksTest {

    @Test
    void gettersSetters_fonctionnentCorrectement() {
        Books livre = new Books();
        livre.setBookId(42);
        livre.setBookName("Test Book");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(10);
        livre.setVersion(3L);

        assertThat(livre.getBookId()).isEqualTo(42);
        assertThat(livre.getBookName()).isEqualTo("Test Book");
        assertThat(livre.getBookAuthor()).isEqualTo("Auteur");
        assertThat(livre.getBookGenre()).isEqualTo("Roman");
        assertThat(livre.getNoOfCopies()).isEqualTo(10);
        assertThat(livre.getVersion()).isEqualTo(3L);
    }

    @Test
    void nouveauLivre_sansVersion_estConsidereCommeNonPersiste() {
        assertThat(new Books().getVersion()).isNull();
    }
}
