package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BadRequestException;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
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
                .isEqualTo(ReservationStatus.EN_ATTENTE);
    }

    // ------------------------------------------------------------------
    // RG-05 / RG-06 : seules EN_ATTENTE et DISPONIBLE sont annulables
    // ------------------------------------------------------------------
    @Test
    void rg05_refuseLAnnulationDuneReservationExpiree() {
        Reservation expiree = reservationDe(1);
        expiree.setStatut(ReservationStatus.EXPIREE);
        when(reservationRepository.findById(60)).thenReturn(Optional.of(expiree));

        assertThatThrownBy(() -> reservationService.annuler(60, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-05");
    }

    @Test
    void rg05_annuleLaReservationActiveDeSonProprietaire() {
        Reservation active = reservationDe(1);
        active.setStatut(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(60)).thenReturn(Optional.of(active));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(reservationService.annuler(60, false, ADHERENT_CONNECTE).getStatut())
                .isEqualTo(ReservationStatus.ANNULEE);
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
    // Tests supplémentaires : creer avec livreId null → BadRequest
    // ------------------------------------------------------------------
    @Test
    void creer_avecLivreIdNull_lanceBadRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setLivreId(null);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("livreId est obligatoire");
    }

    @Test
    void creer_bibliothecaireAvecAdherentIdNull_lanceBadRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(null);

        assertThatThrownBy(() -> reservationService.creer(request, true, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("adherentId est obligatoire");
    }

    @Test
    void creer_livreIntrouvable_lanceNotFound() {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(999);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre");
    }

    @Test
    void creer_adherentIntrouvable_lanceForbidden() {
        when(usersRepository.findByUsername(ADHERENT_CONNECTE)).thenReturn(Optional.empty());

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(ForbiddenException.class);
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : supprimer
    // ------------------------------------------------------------------
    @Test
    void supprimer_supprimeLaReservation() {
        Reservation reservation = reservationDe(1);
        when(reservationRepository.findById(50)).thenReturn(Optional.of(reservation));

        reservationService.supprimer(50);

        verify(reservationRepository).delete(reservation);
    }

    @Test
    void supprimer_reservationIntrouvable_lanceNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.supprimer(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Réservation avec id 999");
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : listerExpirees
    // ------------------------------------------------------------------
    @Test
    void listerExpirees_renvoieLesReservationsExpirees() {
        Reservation expiree = reservationDe(1);
        expiree.setStatut(ReservationStatus.EXPIREE);
        when(reservationRepository.findByStatut(ReservationStatus.EXPIREE))
                .thenReturn(Collections.singletonList(expiree));

        List<ReservationResponse> resultat = reservationService.listerExpirees();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getStatut()).isEqualTo(ReservationStatus.EXPIREE);
    }

    @Test
    void listerExpirees_renvoieListeVideSiAucuneExpiree() {
        when(reservationRepository.findByStatut(ReservationStatus.EXPIREE))
                .thenReturn(Collections.emptyList());

        List<ReservationResponse> resultat = reservationService.listerExpirees();

        assertThat(resultat).isEmpty();
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : expirerReservationsDepassees
    // ------------------------------------------------------------------
    @Test
    void expirerReservationsDepassees_passeLesReservationsEnExpiree() {
        Reservation aExpirer = reservationDe(1);
        aExpirer.setStatut(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findByStatutInAndDateExpirationBefore(any(), any()))
                .thenReturn(Collections.singletonList(aExpirer));

        reservationService.expirerReservationsDepassees();

        verify(reservationRepository).saveAll(any());
        assertThat(aExpirer.getStatut()).isEqualTo(ReservationStatus.EXPIREE);
    }

    @Test
    void expirerReservationsDepassees_neRienSiAucuneExpiree() {
        when(reservationRepository.findByStatutInAndDateExpirationBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        reservationService.expirerReservationsDepassees();

        verify(reservationRepository).saveAll(Collections.emptyList());
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : lister avec filtres
    // ------------------------------------------------------------------
    @Test
    void lister_avecStatutEtAdherent_rechercheStatutEtAdherent() {
        reservationService.lister(ReservationStatus.EN_ATTENTE, 1, true, "admin");

        verify(reservationRepository).findByStatutAndAdherent_UserId(ReservationStatus.EN_ATTENTE, 1);
    }

    @Test
    void lister_avecStatutSeul_rechercheParStatut() {
        reservationService.lister(ReservationStatus.DISPONIBLE, null, true, "admin");

        verify(reservationRepository).findByStatut(ReservationStatus.DISPONIBLE);
    }

    @Test
    void lister_sansFiltre_rechercheTout() {
        when(reservationRepository.findAll()).thenReturn(Collections.emptyList());

        reservationService.lister(null, null, true, "admin");

        verify(reservationRepository).findAll();
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : annuler une réservation DISPONIBLE
    // ------------------------------------------------------------------
    @Test
    void rg06_annuleLaReservationDisponibleDeSonProprietaire() {
        Reservation disponible = reservationDe(1);
        disponible.setStatut(ReservationStatus.DISPONIBLE);
        when(reservationRepository.findById(60)).thenReturn(Optional.of(disponible));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(reservationService.annuler(60, false, ADHERENT_CONNECTE).getStatut())
                .isEqualTo(ReservationStatus.ANNULEE);
    }

    @Test
    void rg05_refuseLAnnulationDUneReservationAnnulee() {
        Reservation annulee = reservationDe(1);
        annulee.setStatut(ReservationStatus.ANNULEE);
        when(reservationRepository.findById(60)).thenReturn(Optional.of(annulee));

        assertThatThrownBy(() -> reservationService.annuler(60, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-05");
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : reservation introuvable
    // ------------------------------------------------------------------
    @Test
    void consulter_reservationIntrouvable_lanceNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.consulter(999, false, ADHERENT_CONNECTE))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Réservation avec id 999");
    }

    @Test
    void annuler_reservationIntrouvable_lanceNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.annuler(999, false, ADHERENT_CONNECTE))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Réservation avec id 999");
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : bibliothecaire ignores le filtre adherentId
    // ------------------------------------------------------------------
    @Test
    void rs05_bibliothecaireSansFiltreListeToutes() {
        when(reservationRepository.findAll())
                .thenReturn(Collections.singletonList(reservationDe(1)));

        List<ReservationResponse> resultat = reservationService.lister(null, null, true, "admin");

        verify(reservationRepository).findAll();
        assertThat(resultat).hasSize(1);
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : creer avec succès
    // ------------------------------------------------------------------
    @Test
    void creer_avecSucces_renvoieStatutEnAttente() {
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        ReservationResponse response = reservationService.creer(request, false, ADHERENT_CONNECTE);

        assertThat(response.getStatut()).isEqualTo(ReservationStatus.EN_ATTENTE);
        assertThat(response.getLivreId()).isEqualTo(101);
        assertThat(response.getAdherentId()).isEqualTo(1);
        assertThat(response.getDateExpiration()).isAfter(response.getDateReservation());
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
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        return reservation;
    }
}
