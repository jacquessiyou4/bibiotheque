package com.ibizabroker.bibliotheque.catalogue.internal;

import lombok.Data;

import javax.persistence.*;

/**
 * Livre persisté. La validation des saisies est portée par BookRequest :
 * l'entité n'est plus reçue directement par l'API. Le stock ne change que
 * par les requêtes atomiques de BooksRepository.
 */
@Data
@Entity
@Table(name = "books")
public class Books {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer bookId;

    String bookName;

    String bookAuthor;

    String bookGenre;

    Integer noOfCopies;

    // Verrouillage optimiste : une modification du livre faite sur une version
    // périmée (stock changé entre-temps par un emprunt) échoue en 409.
    @Version
    Long version;

}
