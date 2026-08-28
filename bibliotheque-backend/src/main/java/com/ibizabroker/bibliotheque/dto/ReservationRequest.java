package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReservationRequest {

    @Schema(description = "Id du livre à réserver (bookId)", example = "1")
    private Integer livreId;

    @Schema(description = "Id de l'adhérent qui réserve (userId)", example = "2")
    private Integer adherentId;
}
