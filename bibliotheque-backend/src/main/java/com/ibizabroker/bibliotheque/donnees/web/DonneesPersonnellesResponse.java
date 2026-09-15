package com.ibizabroker.bibliotheque.donnees.web;

import com.ibizabroker.bibliotheque.emprunts.api.BorrowResponse;
import com.ibizabroker.bibliotheque.reservations.api.ReservationResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;

import java.time.Instant;
import java.util.List;

/**
 * Export des données personnelles d'un adhérent (RGPD, droits d'accès et de
 * portabilité) : tout ce que l'application conserve sur lui, dans un format lisible.
 */
public class DonneesPersonnellesResponse {

    private ProfileResponse profil;
    private List<BorrowResponse> emprunts;
    private List<ReservationResponse> reservations;
    private Instant exporteLe;

    public DonneesPersonnellesResponse() {}

    public DonneesPersonnellesResponse(ProfileResponse profil, List<BorrowResponse> emprunts,
                                       List<ReservationResponse> reservations, Instant exporteLe) {
        this.profil = profil;
        this.emprunts = emprunts;
        this.reservations = reservations;
        this.exporteLe = exporteLe;
    }

    public ProfileResponse getProfil() { return profil; }
    public List<BorrowResponse> getEmprunts() { return emprunts; }
    public List<ReservationResponse> getReservations() { return reservations; }
    public Instant getExporteLe() { return exporteLe; }
}
