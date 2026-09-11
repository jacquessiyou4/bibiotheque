package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    private static final String ADHERENT_CONNECTE = "A1";

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Books livreIndisponible;
    private Users adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new Books();
        livreIndisponible.setBookId(101);
        livreIndisponible.setBookName("Livre Indisponible");
        livreIndisponible.setNoOfCopies(0);

        adherent = new Users();
        adherent.setUserId(1);
        adherent.setUsername(ADHERENT_CONNECTE);
        adherent.setName("Adherent Test");

        when(usersRepository.findByUsername(ADHERENT_CONNECTE)).thenReturn(Optional.of(adherent));
        when(booksRepository.findById(101)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(1)).thenReturn(Optional.of(adherent));
        when(reservationRepository.findByLivre_BookIdAndAdherent_UserIdAndStatutIn(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findByAdherent_UserId(anyInt())).thenReturn(Collections.emptyList());
    }

    // ------------------------------------------------------------------
    // RG-01 : un livre disponible ne peut pas être réservé
    // ------------------------------------------------------------------
    @Test
    void rg01_refuseLaReservationDUnLivreDisponible() {
        Books livreDisponible = new Books();
        livreDisponible.setBookId(202);
        livreDisponible.setBookName("Livre Disponible");
        livreDisponible.setNoOfCopies(2);
        when(booksRepository.findById(202)).thenReturn(Optional.of(livreDisponible));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(202);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-01");
    }

    // ------------------------------------------------------------------
    // RG-02 : pas de double réservation active sur le même livre
    // ------------------------------------------------------------------
    @Test
    void rg02_refuseLaDoubleReservationActiveSurLeMemeLivre() {
        when(reservationRepository.findByLivre_BookIdAndAdherent_UserIdAndStatutIn(any(), any(), any()))
                .thenReturn(Collections.singletonList(reservationDe(1)));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-02");
    }

    // ------------------------------------------------------------------
    // RG-03 : au maximum 3 réservations actives par adhérent
    // ------------------------------------------------------------------
    @Test
    void rg03_refuseLaQuatriemeReservationActive() {
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any()))
                .thenReturn(3L);

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-03");
    }

    @Test
    void rg03_autoriseLaTroisiemeReservationActive() {
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any()))
                .thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        assertThat(reservationService.creer(request, false, ADHERENT_CONNECTE).getStatut())
                .isEqualTo(StatutReservation.EN_ATTENTE);
    }

    // ------------------------------------------------------------------
    // RG-05 / RG-06 : seules EN_ATTENTE et DISPONIBLE sont annulables
    // ------------------------------------------------------------------
    @Test
    void rg05_refuseLAnnulationDuneReservationExpiree() {
        Reservation expiree = reservationDe(1);
        expiree.setStatut(StatutReservation.EXPIREE);
        when(reservationRepository.findById(60)).thenReturn(Optional.of(expiree));

        assertThatThrownBy(() -> reservationService.annuler(60, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-05");
    }

    @Test
    void rg05_annuleLaReservationActiveDeSonProprietaire() {
        Reservation active = reservationDe(1);
        active.setStatut(StatutReservation.EN_ATTENTE);
        when(reservationRepository.findById(60)).thenReturn(Optional.of(active));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(reservationService.annuler(60, false, ADHERENT_CONNECTE).getStatut())
                .isEqualTo(StatutReservation.ANNULEE);
    }

    // ------------------------------------------------------------------
    // RS-04 : l'identité vient du token, pas du corps de la requête
    // ------------------------------------------------------------------
    @Test
    void rs04_adherentNePeutCreerQuePourLuiMeme() {
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(999);

        reservationService.creer(request, false, ADHERENT_CONNECTE);

        // Le user 999 envoyé dans le corps est ignoré : la réservation est
        // rattachée à l'utilisateur du token (user local 1).
        verify(reservationRepository).save(org.mockito.ArgumentMatchers.argThat(r ->
                r.getAdherent().getUserId().equals(1)));
        verify(reservationRepository, never()).save(org.mockito.ArgumentMatchers.argThat(r ->
                r.getAdherent().getUserId().equals(999)));
    }

    @Test
    void rs04_bibliothecairePeutCreerPourUnAutreAdherent() {
        Users autreAdherent = new Users();
        autreAdherent.setUserId(5);
        autreAdherent.setName("Autre Adherent");
        when(usersRepository.findById(5)).thenReturn(Optional.of(autreAdherent));
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(5);

        reservationService.creer(request, true, "admin");

        verify(reservationRepository).save(org.mockito.ArgumentMatchers.argThat(r ->
                r.getAdherent().getUserId().equals(5)));
    }

    // ------------------------------------------------------------------
    // RS-03 : un ADHERENT n'accède qu'à ses propres réservations
    // ------------------------------------------------------------------
    @Test
    void rs03_adherentNePeutPasConsulterLaReservationDUnAutre() {
        Reservation autre = reservationDe(2);
        when(reservationRepository.findById(50)).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> reservationService.consulter(50, false, ADHERENT_CONNECTE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rs03_adherentConsulteSaPropreReservation() {
        Reservation propre = reservationDe(1);
        when(reservationRepository.findById(50)).thenReturn(Optional.of(propre));

        assertThat(reservationService.consulter(50, false, ADHERENT_CONNECTE).getAdherentId())
                .isEqualTo(1);
    }

    @Test
    void rs03_bibliothecaireConsulteLaReservationDUnAutre() {
        Reservation autre = reservationDe(2);
        when(reservationRepository.findById(50)).thenReturn(Optional.of(autre));

        assertThat(reservationService.consulter(50, true, "admin").getAdherentId())
                .isEqualTo(2);
    }

    @Test
    void rs03_adherentNePeutPasAnnulerLaReservationDUnAutre() {
        Reservation autre = reservationDe(2);
        when(reservationRepository.findById(50)).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> reservationService.annuler(50, false, ADHERENT_CONNECTE))
                .isInstanceOf(ForbiddenException.class);
    }

    // ------------------------------------------------------------------
    // RS-05 : un ADHERENT ne voit que ses réservations
    // ------------------------------------------------------------------
    @Test
    void rs05_adherentNeRecoitQueSesReservations() {
        reservationService.lister(null, 999, false, ADHERENT_CONNECTE);

        verify(reservationRepository).findByAdherent_UserId(1);
        verify(reservationRepository, never()).findByAdherent_UserId(999);
    }

    @Test
    void rs05_retourneBienLesReservationsDuConnecte() {
        when(reservationRepository.findByAdherent_UserId(1))
                .thenReturn(Collections.singletonList(reservationDe(1)));

        List<ReservationResponse> resultat =
                reservationService.lister(null, null, false, ADHERENT_CONNECTE);

        assertThat(resultat).extracting(ReservationResponse::getAdherentId).containsExactly(1);
    }

    @Test
    void rs05_bibliothecairePeutFiltrerParAdherent() {
        reservationService.lister(null, 2, true, "admin");

        verify(reservationRepository).findByAdherent_UserId(2);
    }

    // ------------------------------------------------------------------

    private Reservation reservationDe(int adherentUserId) {
        Books livre = new Books();
        livre.setBookId(101);
        livre.setBookName("Livre");
        Users adh = new Users();
        adh.setUserId(adherentUserId);
        adh.setName("Adherent " + adherentUserId);
        Reservation reservation = new Reservation();
        reservation.setId(50);
        reservation.setLivre(livre);
        reservation.setAdherent(adh);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setDateExpiration(LocalDateTime.now().plusDays(7));
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        return reservation;
    }
}