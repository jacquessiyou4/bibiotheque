package com.ibizabroker.bibliotheque.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "adherent")
public class Adherent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Id de l'adhérent (généré automatiquement). Exemple : 1")
    private Integer userId;
    @Schema(description = "Nom d'utilisateur. Exemple : jean.dupont")
    private String username;
    @Schema(description = "Nom complet affiché. Exemple : Jean Dupont")
    private String name;
    @Schema(description = "Mot de passe (BCrypt en base). Exemple : user123")
    private String password;
    @Schema(description = "Matricule optionnel. Exemple : A1")
    private String matricule;

}
