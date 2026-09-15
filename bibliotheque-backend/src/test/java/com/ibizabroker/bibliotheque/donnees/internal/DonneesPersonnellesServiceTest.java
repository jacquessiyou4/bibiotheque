package com.ibizabroker.bibliotheque.donnees.internal;

import com.ibizabroker.bibliotheque.donnees.web.DonneesPersonnellesResponse;
import com.ibizabroker.bibliotheque.emprunts.api.BorrowResponse;
import com.ibizabroker.bibliotheque.emprunts.api.EmpruntsApi;
import com.ibizabroker.bibliotheque.reservations.api.ReservationResponse;
import com.ibizabroker.bibliotheque.reservations.api.ReservationStatus;
import com.ibizabroker.bibliotheque.reservations.api.ReservationsApi;
import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UserResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** La fonctionnalité données personnelles ne connaît les autres que par leurs API. */
@ExtendWith(MockitoExtension.class)
class DonneesPersonnellesServiceTest {

    private static final Authentication A1 = new TestingAuthenticationToken("a1", null, "ROLE_ADHERENT");

    @Mock
    private UtilisateursApi utilisateursApi;

    @Mock
    private EmpruntsApi empruntsApi;

    @Mock
    private ReservationsApi reservationsApi;

    @InjectMocks
    private DonneesPersonnellesService service;

    @Test
    void exporter_rassembleProfilEmpruntsEtReservationsDeLUtilisateurDuJeton() {
        ProfileResponse profil = new ProfileResponse(2, "a1", "Adhérent Un", "a1@bibliotheque.local",
                Collections.singletonList("ADHERENT"));
        when(utilisateursApi.profilCourant(A1)).thenReturn(profil);
        BorrowResponse emprunt = new BorrowResponse(5, 1, 2, LocalDateTime.of(2026, 9, 1, 10, 0), null, null);
        when(empruntsApi.empruntsDe(2)).thenReturn(Collections.singletonList(emprunt));
        ReservationResponse reservation = new ReservationResponse(7, 4, "Une si longue lettre", 2, "Adhérent Un",
                LocalDateTime.of(2026, 9, 2, 9, 0), LocalDateTime.of(2026, 9, 9, 9, 0), ReservationStatus.EN_ATTENTE);
        when(reservationsApi.reservationsDe(2)).thenReturn(Collections.singletonList(reservation));

        DonneesPersonnellesResponse export = service.exporter(A1);

        assertThat(export.getProfil()).isSameAs(profil);
        assertThat(export.getEmprunts()).containsExactly(emprunt);
        assertThat(export.getReservations()).containsExactly(reservation);
        assertThat(export.getExporteLe()).isNotNull();
    }

    @Test
    void anonymiser_delegueALaFonctionnaliteUtilisateurs() {
        UserResponse anonyme = new UserResponse(2, "anonyme-2", "Utilisateur anonymisé", Collections.emptyList());
        when(utilisateursApi.anonymiser(2)).thenReturn(anonyme);

        assertThat(service.anonymiser(2)).isSameAs(anonyme);
    }
}
