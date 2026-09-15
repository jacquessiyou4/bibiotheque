package com.ibizabroker.bibliotheque.emprunts.api;

import java.util.List;

/** Ce que la fonctionnalité emprunts offre aux autres (export des données personnelles). */
public interface EmpruntsApi {

    /** Emprunts d'un utilisateur, rendus ou non. */
    List<BorrowResponse> empruntsDe(Integer userId);
}
