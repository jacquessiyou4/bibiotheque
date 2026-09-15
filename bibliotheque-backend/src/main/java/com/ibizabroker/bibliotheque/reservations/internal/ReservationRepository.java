package com.ibizabroker.bibliotheque.reservations.internal;

import com.ibizabroker.bibliotheque.reservations.api.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByStatut(ReservationStatus statut);

    List<Reservation> findByAdherentId(Integer adherentId);

    List<Reservation> findByStatutAndAdherentId(ReservationStatus statut, Integer adherentId);

    long countByAdherentIdAndStatutIn(Integer adherentId, List<ReservationStatus> statuts);

    List<Reservation> findByLivreIdAndAdherentIdAndStatutIn(Integer livreId, Integer adherentId, List<ReservationStatus> statuts);

    /**
     * Expire en une requête toutes les réservations actives dépassées, au lieu
     * de les charger puis de les sauvegarder une à une.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Reservation r SET r.statut = :expiree, r.version = r.version + 1 "
            + "WHERE r.statut IN :actifs AND r.dateExpiration < :instant")
    int expirer(@Param("actifs") List<ReservationStatus> actifs,
                @Param("expiree") ReservationStatus expiree,
                @Param("instant") LocalDateTime instant);
}
