package com.ibizabroker.bibliotheque.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "livre")
public class Livre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Id du livre (généré automatiquement, ne pas fournir à la création). Exemple : 1")
    Integer bookId;
    @Schema(description = "Titre du livre. Exemple : Le Petit Prince")
    String bookName;
    @Schema(description = "Auteur du livre. Exemple : Antoine de Saint-Exupéry")
    String bookAuthor;
    @Schema(description = "Genre du livre. Exemple : Conte")
    String bookGenre;
    @Schema(description = "Nombre de copies disponibles. Exemple : 2")
    Integer noOfCopies;
    @Schema(description = "Matricule optionnel du livre. Exemple : L1")
    String matricule;

    public void borrowBook() {
        this.noOfCopies--;
    }

    public void returnBook() {
        this.noOfCopies++;
    }

    @Transient
    public Integer getStatus() {
        return (noOfCopies != null && noOfCopies >= 1) ? 1 : 0;
    }

}
