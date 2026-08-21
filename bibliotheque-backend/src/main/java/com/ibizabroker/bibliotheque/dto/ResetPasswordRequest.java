package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Schema(description = "Reset token obtenu via /forgot-password. Exemple : eyJhbGciOiJIUzUxMiJ9...")
    private String resetToken;
    @Schema(description = "Nouveau mot de passe en clair (sera chiffré en BCrypt). Exemple : nouveauMotDePasse123")
    private String newPassword;
}
