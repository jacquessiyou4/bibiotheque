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
import com.ibizabroker.bibliotheque.exceptions.BadRequestException;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private static final List<StatutReservation> STATUTS_ACTIFS =
            Arrays.asList(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE);

    private static final long MAX_RESERVATIONS_ACTIVES = 3;

    private final ReservationRepository reservationRepository;
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;

    public ReservationService(ReservationRepository reservationRepository, BooksRepository booksRepository, UsersRepository usersRepository) {
        this.reservationRepository = reservationRepository;
        this.booksRepository = booksRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * Crée une réservation. L'identité de l'adhérent vient du token
     * (RS-04) : un ADHERENT ne peut réserver que pour lui-même, le
     * champ adherentId du corps de la requête est alors ignoré.
     * Seul un BIBLIOTHECAIRE peut créer une réservation pour autrui.
     */
    public ReservationResponse creer(ReservationRequest request, boolean estBibliothecaire, String usernameConnecte) {
        if (request.getLivreId() == null) {
            throw new BadRequestException("livreId est obligatoire");
        }

        Integer adherentCibleId;
        if (estBibliothecaire) {
            if (request.getAdherentId() == null) {
                throw new BadRequestException("adherentId est obligatoire");
            }
            adherentCibleId = request.getAdherentId();
        } else {
            adherentCibleId = resoudreAdherentConnecte(usernameConnecte).getUserId();
        }

        Books livre = booksRepository.findById(request.getLivreId())
                .orElseThrow(() -> new NotFoundException("Livre avec id " + request.getLivreId() + " introuvable."));
        Users adherent = usersRepository.findById(adherentCibleId)
                .orElseThrow(() -> new NotFoundException("Adhérent avec id " + adherentCibleId + " introuvable."));

        if (livre.getNoOfCopies() != null && livre.getNoOfCopies() >= 1) {
            throw new ConflictException("RG-01: le livre \"" + livre.getBookName() + "\" est disponible, la réservation est refusée.");
        }

        boolean dejaReserve = !reservationRepository
                .findByLivre_BookIdAndAdherent_UserIdAndStatutIn(livre.getBookId(), adherent.getUserId(), STATUTS_ACTIFS)
                .isEmpty();
        if (dejaReserve) {
            throw new ConflictException("RG-02: cet adhérent a déjà une réservation active sur ce livre.");
        }

        long reservationsActives = reservationRepository
                .countByAdherent_UserIdAndStatutIn(adherent.getUserId(), STATUTS_ACTIFS);
        if (reservationsActives >= MAX_RESERVATIONS_ACTIVES) {
            throw new ConflictException("RG-03: cet adhérent a déjà " + MAX_RESERVATIONS_ACTIVES + " réservations actives, la limite est atteinte.");
        }

        LocalDateTime maintenant = LocalDateTime.now();

        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(maintenant);
        reservation.setDateExpiration(maintenant.plusDays(7));
        reservation.setStatut(StatutReservation.EN_ATTENTE);

        Reservation saved = reservationRepository.save(reservation);
        return versDto(saved);
    }

    /**
     * Liste les réservations. Un ADHERENT ne voit que les siennes (RS-05),
     * même s'il tente de filtrer via adherentId. Un BIBLIOTHECAIRE peut
     * tout voir et filtrer par adhérent.
     */
    public List<ReservationResponse> lister(StatutReservation statut, Integer adherentId,
                                            boolean estBibliothecaire, String usernameConnecte) {
        List<Reservation> reservations;
        if (!estBibliothecaire) {
            adherentId = resoudreAdherentConnecte(usernameConnecte).getUserId();
        }
        if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByStatutAndAdherent_UserId(statut, adherentId);
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherent_UserId(adherentId);
        } else {
            reservations = reservationRepository.findAll();
        }
        return reservations.stream().map(this::versDto).collect(Collectors.toList());
    }

    /**
     * Consulte une réservation. Un ADHERENT n'y accède que si elle lui
     * appartient (RS-03), sinon 403.
     */
    public ReservationResponse consulter(Integer id, boolean estBibliothecaire, String usernameConnecte) {
        Reservation reservation = trouverParId(id);
        verifierPropriete(reservation, estBibliothecaire, usernameConnecte);
        return versDto(reservation);
    }

    public List<ReservationResponse> listerExpirees() {
        return reservationRepository.findByStatut(StatutReservation.EXPIREE).stream()
                .map(this::versDto)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelayString = "60000")
    public void expirerReservationsDepassees() {
        List<Reservation> aExpirer = reservationRepository
                .findByStatutInAndDateExpirationBefore(STATUTS_ACTIFS, LocalDateTime.now());
        if (!aExpirer.isEmpty()) {
            log.info("Expiration de {} réservation(s) dépassées", aExpirer.size());
        }
        aExpirer.forEach(r -> r.setStatut(StatutReservation.EXPIREE));
        reservationRepository.saveAll(aExpirer);
    }

    /**
     * Annule une réservation. Un ADHERENT ne peut annuler que la sienne
     * (RS-03), sinon 403.
     */
    public ReservationResponse annuler(Integer id, boolean estBibliothecaire, String usernameConnecte) {
        Reservation reservation = trouverParId(id);
        verifierPropriete(reservation, estBibliothecaire, usernameConnecte);

        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new ConflictException("RG-05/RG-06: la réservation est au statut " + reservation.getStatut()
                    + ", seules EN_ATTENTE ou DISPONIBLE peuvent être annulées.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        Reservation saved = reservationRepository.save(reservation);
        return versDto(saved);
    }

    public void supprimer(Integer id) {
        reservationRepository.delete(trouverParId(id));
    }

    private Reservation trouverParId(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation avec id " + id + " introuvable."));
    }

    /**
     * Vérifie la propriété d'une réservation : un BIBLIOTHECAIRE passe
     * toujours, un ADHERENT uniquement si la réservation lui appartient.
     */
    private void verifierPropriete(Reservation reservation, boolean estBibliothecaire, String usernameConnecte) {
        if (estBibliothecaire) {
            return;
        }
        Users adherent = resoudreAdherentConnecte(usernameConnecte);
        if (!adherent.getUserId().equals(reservation.getAdherent().getUserId())) {
            throw new ForbiddenException("Accès refusé : cette réservation ne vous appartient pas.");
        }
    }

    /**
     * Résout l'utilisateur local de l'application à partir du compte
     * Keycloak authentifié (preferred_username du token).
     */
    private Users resoudreAdherentConnecte(String usernameConnecte) {
        return usersRepository.findByUsername(usernameConnecte)
                .orElseThrow(() -> new ForbiddenException(
                        "Aucun compte adhérent local pour le compte Keycloak « " + usernameConnecte + " »."));
    }

    private ReservationResponse versDto(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getLivre().getBookId(),
                reservation.getLivre().getBookName(),
                reservation.getAdherent().getUserId(),
                reservation.getAdherent().getName(),
                reservation.getDateReservation(),
                reservation.getDateExpiration(),
                reservation.getStatut()
        );
    }
}