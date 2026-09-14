package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByStatut(ReservationStatus statut);

    List<Reservation> findByAdherent_UserId(Integer adherentId);

    List<Reservation> findByStatutAndAdherent_UserId(ReservationStatus statut, Integer adherentId);

    long countByAdherent_UserIdAndStatutIn(Integer adherentId, List<ReservationStatus> statuts);

    List<Reservation> findByLivre_BookIdAndAdherent_UserIdAndStatutIn(Integer livreId, Integer adherentId, List<ReservationStatus> statuts);

    List<Reservation> findByStatutInAndDateExpirationBefore(List<ReservationStatus> statuts, LocalDateTime instant);
}
