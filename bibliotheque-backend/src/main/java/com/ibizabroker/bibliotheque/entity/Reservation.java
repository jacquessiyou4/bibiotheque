package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "livre_id", nullable = false)
    private Books livre;

    @ManyToOne
    @JoinColumn(name = "adherent_id", nullable = false)
    private Users adherent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut;
}
