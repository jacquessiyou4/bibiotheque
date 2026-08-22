package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Livre livre;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Adherent adherent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut;

}
