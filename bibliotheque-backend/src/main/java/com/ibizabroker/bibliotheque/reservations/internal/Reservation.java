package com.ibizabroker.bibliotheque.reservations.internal;

import com.ibizabroker.bibliotheque.reservations.api.ReservationStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Réservation d'un livre indisponible. Le livre et l'adhérent appartiennent
 * à d'autres fonctionnalités : seuls leurs identifiants sont conservés ici
 * (les clés étrangères de la base garantissent qu'ils existent).
 */
@Data
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "livre_id", nullable = false)
    private Integer livreId;

    @Column(name = "adherent_id", nullable = false)
    private Integer adherentId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus statut;

    // Verrouillage optimiste : deux mises à jour concurrentes ne s'écrasent pas.
    @Version
    private Long version;
}
