package com.ibizabroker.bibliotheque.reservations.internal;

import com.ibizabroker.bibliotheque.shared.config.ReglesBibliotheque;
import com.ibizabroker.bibliotheque.shared.observabilite.MetriquesMetier;

import com.ibizabroker.bibliotheque.catalogue.api.CatalogueApi;
import com.ibizabroker.bibliotheque.catalogue.api.LivreResume;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateurResume;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import com.ibizabroker.bibliotheque.reservations.web.ReservationRequest;
import com.ibizabroker.bibliotheque.reservations.api.ReservationResponse;
import com.ibizabroker.bibliotheque.reservations.api.ReservationStatus;
import com.ibizabroker.bibliotheque.shared.error.BadRequestException;
import com.ibizabroker.bibliotheque.shared.error.ConflictException;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private CatalogueApi catalogueApi;

    @Mock
    private UtilisateursApi utilisateursApi;

    // Règles par défaut (7 jours, 3 réservations actives), comme en production.
    @Spy
    private ReglesBibliotheque regles = new ReglesBibliotheque();

    @Mock
    private MetriquesMetier metriques;

    @InjectMocks
    private ReservationService reservationService;

    private LivreResume livreIndisponible;
    private UtilisateurResume adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new LivreResume(101, "Livre Indisponible", 0);
        adherent = new UtilisateurResume(1, ADHERENT_CONNECTE, "Adherent Test");

        when(utilisateursApi.parUsername(ADHERENT_CONNECTE)).thenReturn(Optional.of(adherent));
        when(catalogueApi.livre(101)).thenReturn(Optional.of(livreIndisponible));
        when(utilisateursApi.utilisateur(1)).thenReturn(Optional.of(adherent));
        when(reservationRepository.findByLivreIdAndAdherentIdAndStatutIn(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findByAdherentId(anyInt())).thenReturn(Collections.emptyList());
    }

    // ------------------------------------------------------------------
    // RG-01 : un livre disponible ne peut pas être réservé
    // ------------------------------------------------------------------
    @Test
    void rg01_refuseLaReservationDUnLivreDisponible() {
        when(catalogueApi.livre(202)).thenReturn(Optional.of(new LivreResume(202, "Livre Disponible", 2)));

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
        when(reservationRepository.findByLivreIdAndAdherentIdAndStatutIn(any(), any(), any()))
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
        when(reservationRepository.countByAdherentIdAndStatutIn(anyInt(), any()))
                .thenReturn(3L);

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-03");
    }

    @Test
    void rg03_limiteConfigurable() {
        regles.setReservationsActivesMax(5);
        when(reservationRepository.countByAdherentIdAndStatutIn(anyInt(), any())).thenReturn(4L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);

        assertThat(reservationService.creer(request, false, ADHERENT_CONNECTE).getStatut())
                .isEqualTo(ReservationStatus.EN_ATTENTE);
        verify(metriques).reservationCreee();
    }

    @Test
    void rg03_autoriseLaTroisiemeReservationActive() {
        when(reservationRepository.countByAdherentIdAndStatutIn(anyInt(), any()))
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
        when(reservationRepository.countByAdherentIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(999);

        reservationService.creer(request, false, ADHERENT_CONNECTE);

        verify(reservationRepository).save(org.mockito.ArgumentMatchers.argThat(r ->
                r.getAdherentId().equals(1)));
        verify(reservationRepository, never()).save(org.mockito.ArgumentMatchers.argThat(r ->
                r.getAdherentId().equals(999)));
    }

    @Test
    void rs04_bibliothecairePeutCreerPourUnAutreAdherent() {
        when(utilisateursApi.utilisateur(5)).thenReturn(Optional.of(new UtilisateurResume(5, "a5", "Autre Adherent")));
        when(reservationRepository.countByAdherentIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(5);

        reservationService.creer(request, true, "admin");

        verify(reservationRepository).save(org.mockito.ArgumentMatchers.argThat(r ->
                r.getAdherentId().equals(5)));
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

        verify(reservationRepository).findByAdherentId(1);
        verify(reservationRepository, never()).findByAdherentId(999);
    }

    @Test
    void rs05_retourneBienLesReservationsDuConnecte() {
        when(reservationRepository.findByAdherentId(1))
                .thenReturn(Collections.singletonList(reservationDe(1)));

        List<ReservationResponse> resultat =
                reservationService.lister(null, null, false, ADHERENT_CONNECTE);

        assertThat(resultat).extracting(ReservationResponse::getAdherentId).containsExactly(1);
    }

    @Test
    void rs05_bibliothecairePeutFiltrerParAdherent() {
        reservationService.lister(null, 2, true, "admin");

        verify(reservationRepository).findByAdherentId(2);
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
        when(catalogueApi.livre(999)).thenReturn(Optional.empty());

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(999);

        assertThatThrownBy(() -> reservationService.creer(request, false, ADHERENT_CONNECTE))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Livre");
    }

    @Test
    void creer_adherentIntrouvable_lanceForbidden() {
        when(utilisateursApi.parUsername(ADHERENT_CONNECTE)).thenReturn(Optional.empty());

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
    void expirerReservationsDepassees_expireLesActivesEnUneSeuleRequete() {
        when(reservationRepository.expirer(any(), any(), any())).thenReturn(2);

        reservationService.expirerReservationsDepassees();

        verify(reservationRepository).expirer(
                eq(Arrays.asList(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)),
                eq(ReservationStatus.EXPIREE),
                any(LocalDateTime.class));
        verify(metriques).reservationsExpirees(2);
        verify(reservationRepository, never()).saveAll(any());
    }

    @Test
    void expirerReservationsDepassees_sansReservationDepassee_neModifieRien() {
        when(reservationRepository.expirer(any(), any(), any())).thenReturn(0);

        reservationService.expirerReservationsDepassees();

        verify(reservationRepository, never()).saveAll(any());
    }

    // ------------------------------------------------------------------
    // Tests supplémentaires : lister avec filtres
    // ------------------------------------------------------------------
    @Test
    void lister_avecStatutEtAdherent_rechercheStatutEtAdherent() {
        reservationService.lister(ReservationStatus.EN_ATTENTE, 1, true, "admin");

        verify(reservationRepository).findByStatutAndAdherentId(ReservationStatus.EN_ATTENTE, 1);
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
        when(reservationRepository.countByAdherentIdAndStatutIn(anyInt(), any())).thenReturn(0L);
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

    @Test
    void lister_renseigneLesNomsDuLivreEtDeLAdherentEnDeuxRequetes() {
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(reservationDe(1), reservationDe(2)));
        when(catalogueApi.livres(any())).thenReturn(Collections.singletonMap(101, livreIndisponible));
        when(utilisateursApi.utilisateurs(any())).thenReturn(Collections.singletonMap(1, adherent));

        List<ReservationResponse> resultat = reservationService.lister(null, null, true, "admin");

        assertThat(resultat).extracting(ReservationResponse::getLivreNom)
                .containsExactly("Livre Indisponible", "Livre Indisponible");
        // Adhérent 2 absent de la réponse de l'API : pas de nom plutôt qu'une erreur.
        assertThat(resultat).extracting(ReservationResponse::getAdherentNom)
                .containsExactly("Adherent Test", null);
        verify(catalogueApi).livres(any());
        verify(utilisateursApi).utilisateurs(any());
    }

    @Test
    void reservationsDe_renvoieToutesLesReservationsDeLAdherent() {
        when(reservationRepository.findByAdherentId(1)).thenReturn(Collections.singletonList(reservationDe(1)));

        assertThat(reservationService.reservationsDe(1)).extracting(ReservationResponse::getAdherentId).containsExactly(1);
    }

    // ------------------------------------------------------------------

    private Reservation reservationDe(int adherentUserId) {
        Reservation reservation = new Reservation();
        reservation.setId(50);
        reservation.setLivreId(101);
        reservation.setAdherentId(adherentUserId);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setDateExpiration(LocalDateTime.now().plusDays(7));
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        return reservation;
    }
}
