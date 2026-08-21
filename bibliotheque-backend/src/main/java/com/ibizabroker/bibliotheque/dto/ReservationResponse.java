package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.StatutReservation;
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
    private String livreMatricule;
    private Integer adherentId;
    private String adherentNom;
    private String adherentMatricule;
    private LocalDateTime dateReservation;
    private LocalDateTime dateExpiration;
    private StatutReservation statut;
}
