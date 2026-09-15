package com.ibizabroker.bibliotheque.catalogue.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BooksRepository extends JpaRepository<Books, Integer> {

    /**
     * Retire une copie en une seule requête : lire le stock puis l'écrire
     * laissait deux emprunts simultanés prendre le dernier exemplaire.
     * La version est incrémentée pour qu'une modification concurrente du
     * livre (écran bibliothécaire) échoue au lieu d'écraser le stock.
     *
     * @return 1 si une copie a été retirée, 0 si le stock était épuisé
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Books b SET b.noOfCopies = b.noOfCopies - 1, b.version = b.version + 1 "
            + "WHERE b.bookId = :bookId AND b.noOfCopies > 0")
    int decrementerStock(@Param("bookId") Integer bookId);

    /** Remet une copie en stock (retour d'un emprunt). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Books b SET b.noOfCopies = b.noOfCopies + 1, b.version = b.version + 1 "
            + "WHERE b.bookId = :bookId")
    int incrementerStock(@Param("bookId") Integer bookId);
}
