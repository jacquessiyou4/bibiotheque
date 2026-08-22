package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @Schema(description = "Nom d'utilisateur du compte pour lequel générer un token de réinitialisation. Exemple : admin")
    private String username;
}
