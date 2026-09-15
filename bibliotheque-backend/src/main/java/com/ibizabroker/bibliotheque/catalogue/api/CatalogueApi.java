package com.ibizabroker.bibliotheque.catalogue.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Ce que le catalogue offre aux autres fonctionnalités (emprunts,
 * réservations). Elles n'accèdent jamais à ses entités ni à ses repositories.
 */
public interface CatalogueApi {

    Optional<LivreResume> livre(Integer bookId);

    /** Livres par identifiant, en une requête (les identifiants inconnus sont absents de la carte). */
    Map<Integer, LivreResume> livres(Collection<Integer> bookIds);

    /**
     * Retire un exemplaire du stock, de façon atomique.
     *
     * @return false si le stock était déjà épuisé (aucun exemplaire retiré)
     */
    boolean retirerExemplaire(Integer bookId);

    /** Remet un exemplaire en stock (retour d'un emprunt). */
    void remettreExemplaire(Integer bookId);
}
