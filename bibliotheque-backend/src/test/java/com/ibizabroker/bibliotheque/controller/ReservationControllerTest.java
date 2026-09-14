package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de ReservationController : ReservationService simulé,
 * aucun contexte Spring. Vérifie que le rôle (bibliothécaire ou non) et
 * l'utilisateur du jeton sont transmis au service, et les statuts HTTP.
 * Les règles RS-01 à RS-05 sont couvertes par ReservationControllerIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    private static final Authentication ADHERENT_A1 = new TestingAuthenticationToken("a1", null, "ROLE_ADHERENT");
    private static final Authentication BIBLIOTHECAIRE =
            new TestingAuthenticationToken("admin", null, "ROLE_Admin", "ROLE_BIBLIOTHECAIRE");

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController controller;

    @Test
    void creer_adherent_transmetLeRoleEtLUtilisateurPuisRenvoie201() {
        ReservationRequest demande = new ReservationRequest();
        demande.setLivreId(4);
        ReservationResponse cree = reservation(7);
        when(reservationService.creer(demande, false, "a1")).thenReturn(cree);

        ResponseEntity<ReservationResponse> reponse = controller.creer(ADHERENT_A1, demande);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reponse.getBody()).isSameAs(cree);
    }

    @Test
    void creer_bibliothecaire_estReconnuCommeTel() {
        ReservationRequest demande = new ReservationRequest();
        demande.setLivreId(4);
        demande.setAdherentId(2);
        when(reservationService.creer(demande, true, "admin")).thenReturn(reservation(8));

        assertThat(controller.creer(BIBLIOTHECAIRE, demande).getBody().getId()).isEqualTo(8);
    }

    @Test
    void lister_transmetLesFiltresLeRoleEtLUtilisateur() {
        List<ReservationResponse> reservations = Collections.singletonList(reservation(7));
        when(reservationService.lister(isNull(), eq(3), eq(false), eq("a1"))).thenReturn(reservations);

        ResponseEntity<List<ReservationResponse>> reponse = controller.lister(ADHERENT_A1, null, 3);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isSameAs(reservations);
    }

    @Test
    void listerExpirees_renvoie200() {
        when(reservationService.listerExpirees()).thenReturn(Collections.emptyList());

        assertThat(controller.listerExpirees().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void consulter_transmetLeRoleEtLUtilisateur() {
        when(reservationService.consulter(7, true, "admin")).thenReturn(reservation(7));

        assertThat(controller.consulter(BIBLIOTHECAIRE, 7).getBody().getId()).isEqualTo(7);
    }

    @Test
    void annuler_transmetLeRoleEtLUtilisateur() {
        when(reservationService.annuler(7, false, "a1")).thenReturn(reservation(7));

        ResponseEntity<ReservationResponse> reponse = controller.annuler(ADHERENT_A1, 7);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().getId()).isEqualTo(7);
    }

    @Test
    void supprimer_supprimeEtRenvoie204() {
        ResponseEntity<Void> reponse = controller.supprimer(BIBLIOTHECAIRE, 7);

        verify(reservationService).supprimer(7);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private ReservationResponse reservation(int id) {
        ReservationResponse reservation = new ReservationResponse();
        reservation.setId(id);
        reservation.setLivreId(4);
        reservation.setAdherentId(2);
        return reservation;
    }
}
