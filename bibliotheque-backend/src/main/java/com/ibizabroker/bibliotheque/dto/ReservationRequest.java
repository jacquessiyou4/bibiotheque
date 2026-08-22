package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReservationRequest {
    @Schema(description = "Id du livre à réserver (bookId). Exemple : 2")
    private Integer livreId;
    @Schema(description = "Id de l'adhérent qui réserve (userId). Exemple : 1")
    private Integer adherentId;
}
