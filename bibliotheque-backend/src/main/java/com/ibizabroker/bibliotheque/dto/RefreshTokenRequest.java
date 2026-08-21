package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @Schema(description = "Refresh token obtenu via /login ou /authenticate. Exemple : eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;
}
