package com.ibizabroker.bibliotheque.reservations.api;

import java.util.List;

/** Ce que la fonctionnalité réservations offre aux autres (export des données personnelles). */
public interface ReservationsApi {

    /** Toutes les réservations d'un adhérent, quel que soit leur statut. */
    List<ReservationResponse> reservationsDe(Integer adherentId);
}
