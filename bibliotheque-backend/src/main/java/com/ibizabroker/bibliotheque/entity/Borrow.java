package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity @EntityListeners(AuditingEntityListener.class)
@Table(name = "Borrow")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Id de l'emprunt (requis uniquement pour PUT /borrow, ignoré à la création). Exemple : 1")
    Integer borrowId;
    @Schema(description = "Id du livre emprunté (bookId). Exemple : 2")
    Integer bookId;
    @Schema(description = "Id de l'adhérent emprunteur (userId). Exemple : 1")
    Integer userId;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    @Schema(description = "Renseigné automatiquement par le serveur à l'emprunt, ne pas fournir")
    Date issueDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    @Schema(description = "Renseigné automatiquement par le serveur au retour, ne pas fournir")
    Date returnDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    @Schema(description = "Renseigné automatiquement par le serveur (échéance à 7 jours), ne pas fournir")
    Date dueDate;

}
