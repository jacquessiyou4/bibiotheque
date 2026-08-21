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
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final List<StatutReservation> STATUTS_ACTIFS =
            Arrays.asList(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE);

    private static final long MAX_RESERVATIONS_ACTIVES = 3;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    public ReservationResponse creer(ReservationRequest request) {
        if (request.getLivreId() == null) {
            throw new BadRequestException("livreId est obligatoire");
        }
        if (request.getAdherentId() == null) {
            throw new BadRequestException("adherentId est obligatoire");
        }

        Books livre = booksRepository.findById(request.getLivreId())
                .orElseThrow(() -> new NotFoundException("Livre avec id " + request.getLivreId() + " introuvable."));
        Users adherent = usersRepository.findById(request.getAdherentId())
                .orElseThrow(() -> new NotFoundException("Adherent avec id " + request.getAdherentId() + " introuvable."));

        if (livre.getNoOfCopies() >= 1) {
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

    public List<ReservationResponse> lister(StatutReservation statut, Integer adherentId) {
        List<Reservation> reservations;
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

    public ReservationResponse consulter(Integer id) {
        return versDto(trouverParId(id));
    }

    public ReservationResponse annuler(Integer id) {
        Reservation reservation = trouverParId(id);

        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new ConflictException("RG-05: la réservation est au statut " + reservation.getStatut()
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
