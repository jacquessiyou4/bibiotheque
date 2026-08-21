package com.ibizabroker.bibliotheque.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "administrator")
public class Administrator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Id de l'administrateur (généré automatiquement). Exemple : 1")
    private Integer userId;
    @Schema(description = "Nom d'utilisateur. Exemple : admin")
    private String username;
    @Schema(description = "Nom complet affiché. Exemple : Administrateur")
    private String name;
    @Schema(description = "Mot de passe (BCrypt en base). Exemple : admin123")
    private String password;
    @Schema(description = "Matricule optionnel. Exemple : ADM1")
    private String matricule;

}
