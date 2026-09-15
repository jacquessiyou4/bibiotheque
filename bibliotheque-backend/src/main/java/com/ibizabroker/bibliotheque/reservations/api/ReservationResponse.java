package com.ibizabroker.bibliotheque.reservations.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private Integer id;
    private Integer livreId;
    private String livreNom;
    private Integer adherentId;
    private String adherentNom;
    private LocalDateTime dateReservation;
    private LocalDateTime dateExpiration;
    private ReservationStatus statut;
}
