package com.ibizabroker.bibliotheque.reservations.internal;

import com.ibizabroker.bibliotheque.catalogue.api.CatalogueApi;
import com.ibizabroker.bibliotheque.catalogue.api.LivreResume;
import com.ibizabroker.bibliotheque.reservations.api.ReservationResponse;
import com.ibizabroker.bibliotheque.reservations.api.ReservationStatus;
import com.ibizabroker.bibliotheque.reservations.api.ReservationsApi;
import com.ibizabroker.bibliotheque.reservations.web.ReservationRequest;
import com.ibizabroker.bibliotheque.shared.config.ReglesBibliotheque;
import com.ibizabroker.bibliotheque.shared.error.BadRequestException;
import com.ibizabroker.bibliotheque.shared.error.ConflictException;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import com.ibizabroker.bibliotheque.shared.observabilite.MetriquesMetier;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateurResume;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReservationService implements ReservationsApi {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private static final List<ReservationStatus> STATUTS_ACTIFS =
            Arrays.asList(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE);

    private final ReservationRepository reservationRepository;
    private final CatalogueApi catalogueApi;
    private final UtilisateursApi utilisateursApi;
    private final ReglesBibliotheque regles;
    private final MetriquesMetier metriques;

    public ReservationService(ReservationRepository reservationRepository, CatalogueApi catalogueApi,
                              UtilisateursApi utilisateursApi, ReglesBibliotheque regles, MetriquesMetier metriques) {
        this.reservationRepository = reservationRepository;
        this.catalogueApi = catalogueApi;
        this.utilisateursApi = utilisateursApi;
        this.regles = regles;
        this.metriques = metriques;
    }

    /**
     * Crée une réservation. L'identité de l'adhérent vient du token
     * (RS-04) : un ADHERENT ne peut réserver que pour lui-même, le
     * champ adherentId du corps de la requête est alors ignoré.
     * Seul un BIBLIOTHECAIRE peut créer une réservation pour autrui.
     */
    @Transactional
    public ReservationResponse creer(ReservationRequest request, boolean estBibliothecaire, String usernameConnecte) {
        if (request.getLivreId() == null) {
            throw new BadRequestException("VALIDATION_FAILED", "livreId est obligatoire");
        }

        Integer adherentCibleId;
        if (estBibliothecaire) {
            if (request.getAdherentId() == null) {
                throw new BadRequestException("VALIDATION_FAILED", "adherentId est obligatoire");
            }
            adherentCibleId = request.getAdherentId();
        } else {
            adherentCibleId = resoudreAdherentConnecte(usernameConnecte).getUserId();
        }

        LivreResume livre = catalogueApi.livre(request.getLivreId())
                .orElseThrow(() -> new NotFoundException("BOOK_NOT_FOUND", "Livre avec id " + request.getLivreId() + " introuvable."));
        UtilisateurResume adherent = utilisateursApi.utilisateur(adherentCibleId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Adhérent avec id " + adherentCibleId + " introuvable."));

        if (livre.estDisponible()) {
            throw new ConflictException("RESERVATION_BOOK_AVAILABLE", "RG-01: le livre \"" + livre.getBookName() + "\" est disponible, la réservation est refusée.");
        }

        boolean dejaReserve = !reservationRepository
                .findByLivreIdAndAdherentIdAndStatutIn(livre.getBookId(), adherent.getUserId(), STATUTS_ACTIFS)
                .isEmpty();
        if (dejaReserve) {
            throw new ConflictException("RESERVATION_DUPLICATE", "RG-02: cet adhérent a déjà une réservation active sur ce livre.");
        }

        long reservationsActives = reservationRepository
                .countByAdherentIdAndStatutIn(adherent.getUserId(), STATUTS_ACTIFS);
        if (reservationsActives >= regles.getReservationsActivesMax()) {
            throw new ConflictException("RESERVATION_LIMIT_REACHED", "RG-03: cet adhérent a déjà " + regles.getReservationsActivesMax() + " réservations actives, la limite est atteinte.");
        }

        LocalDateTime maintenant = LocalDateTime.now();

        Reservation reservation = new Reservation();
        reservation.setLivreId(livre.getBookId());
        reservation.setAdherentId(adherent.getUserId());
        reservation.setDateReservation(maintenant);
        reservation.setDateExpiration(maintenant.plusDays(regles.getDureeReservationJours()));
        reservation.setStatut(ReservationStatus.EN_ATTENTE);

        Reservation saved = reservationRepository.save(reservation);
        metriques.reservationCreee();
        return versDto(saved, livre, adherent);
    }

    /**
     * Liste les réservations. Un ADHERENT ne voit que les siennes (RS-05),
     * même s'il tente de filtrer via adherentId. Un BIBLIOTHECAIRE peut
     * tout voir et filtrer par adhérent.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> lister(ReservationStatus statut, Integer adherentId,
                                            boolean estBibliothecaire, String usernameConnecte) {
        List<Reservation> reservations;
        if (!estBibliothecaire) {
            adherentId = resoudreAdherentConnecte(usernameConnecte).getUserId();
        }
        if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByStatutAndAdherentId(statut, adherentId);
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherentId(adherentId);
        } else {
            reservations = reservationRepository.findAll();
        }
        return versDtos(reservations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> reservationsDe(Integer adherentId) {
        return versDtos(reservationRepository.findByAdherentId(adherentId));
    }

    /**
     * Consulte une réservation. Un ADHERENT n'y accède que si elle lui
     * appartient (RS-03), sinon 403.
     */
    @Transactional(readOnly = true)
    public ReservationResponse consulter(Integer id, boolean estBibliothecaire, String usernameConnecte) {
        Reservation reservation = trouverParId(id);
        verifierPropriete(reservation, estBibliothecaire, usernameConnecte);
        return versDtos(Collections.singletonList(reservation)).get(0);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listerExpirees() {
        return versDtos(reservationRepository.findByStatut(ReservationStatus.EXPIREE));
    }

    // Une seule requête UPDATE, quel que soit le nombre de réservations dépassées.
    @Transactional
    @Scheduled(fixedDelayString = "60000")
    public void expirerReservationsDepassees() {
        int expirees = reservationRepository.expirer(STATUTS_ACTIFS, ReservationStatus.EXPIREE, LocalDateTime.now());
        if (expirees > 0) {
            log.info("Expiration de {} réservation(s) dépassées", expirees);
        }
        metriques.reservationsExpirees(expirees);
    }

    /**
     * Annule une réservation. Un ADHERENT ne peut annuler que la sienne
     * (RS-03), sinon 403.
     */
    @Transactional
    public ReservationResponse annuler(Integer id, boolean estBibliothecaire, String usernameConnecte) {
        Reservation reservation = trouverParId(id);
        verifierPropriete(reservation, estBibliothecaire, usernameConnecte);

        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new ConflictException("RESERVATION_NOT_CANCELLABLE", "RG-05/RG-06: la réservation est au statut " + reservation.getStatut()
                    + ", seules EN_ATTENTE ou DISPONIBLE peuvent être annulées.");
        }

        reservation.setStatut(ReservationStatus.ANNULEE);
        Reservation saved = reservationRepository.save(reservation);
        return versDtos(Collections.singletonList(saved)).get(0);
    }

    @Transactional
    public void supprimer(Integer id) {
        reservationRepository.delete(trouverParId(id));
    }

    private Reservation trouverParId(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Réservation avec id " + id + " introuvable."));
    }

    /**
     * Vérifie la propriété d'une réservation : un BIBLIOTHECAIRE passe
     * toujours, un ADHERENT uniquement si la réservation lui appartient.
     */
    private void verifierPropriete(Reservation reservation, boolean estBibliothecaire, String usernameConnecte) {
        if (estBibliothecaire) {
            return;
        }
        UtilisateurResume adherent = resoudreAdherentConnecte(usernameConnecte);
        if (!adherent.getUserId().equals(reservation.getAdherentId())) {
            throw new ForbiddenException("RESERVATION_NOT_OWNED", "Accès refusé : cette réservation ne vous appartient pas.");
        }
    }

    /**
     * Résout l'utilisateur local de l'application à partir du compte
     * Keycloak authentifié (preferred_username du token).
     */
    private UtilisateurResume resoudreAdherentConnecte(String usernameConnecte) {
        return utilisateursApi.parUsername(usernameConnecte)
                .orElseThrow(() -> new ForbiddenException("LOCAL_ACCOUNT_MISSING",
                        "Aucun compte adhérent local pour le compte Keycloak « " + usernameConnecte + " »."));
    }

    /** Noms des livres et des adhérents chargés en deux requêtes, quel que soit le nombre de réservations. */
    private List<ReservationResponse> versDtos(List<Reservation> reservations) {
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> livreIds = reservations.stream().map(Reservation::getLivreId).collect(Collectors.toSet());
        Set<Integer> adherentIds = reservations.stream().map(Reservation::getAdherentId).collect(Collectors.toSet());
        Map<Integer, LivreResume> livres = catalogueApi.livres(livreIds);
        Map<Integer, UtilisateurResume> adherents = utilisateursApi.utilisateurs(adherentIds);
        return reservations.stream()
                .map(r -> versDto(r, livres.get(r.getLivreId()), adherents.get(r.getAdherentId())))
                .collect(Collectors.toList());
    }

    private ReservationResponse versDto(Reservation reservation, LivreResume livre, UtilisateurResume adherent) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getLivreId(),
                livre != null ? livre.getBookName() : null,
                reservation.getAdherentId(),
                adherent != null ? adherent.getName() : null,
                reservation.getDateReservation(),
                reservation.getDateExpiration(),
                reservation.getStatut()
        );
    }
}
