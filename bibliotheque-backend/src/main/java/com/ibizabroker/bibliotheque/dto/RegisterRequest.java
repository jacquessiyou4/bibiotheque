package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RegisterRequest {
    @Schema(description = "Nom d'utilisateur unique, utilisé pour se connecter. Exemple : marie.curie")
    private String username;
    @Schema(description = "Nom complet affiché. Exemple : Marie Curie")
    private String name;
    @Schema(description = "Mot de passe en clair (sera chiffré en BCrypt). Exemple : motdepasse123")
    private String password;
    @Schema(description = "Matricule optionnel identifiant le compte. Exemple : A4")
    private String matricule;
    @Schema(description = "Type de compte à créer : \"Administrator\" ou \"Adherent\". Exemple : Adherent")
    private String role;
}
