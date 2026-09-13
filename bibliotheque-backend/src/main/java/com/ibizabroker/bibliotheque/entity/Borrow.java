package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Entity
@Table(name = "borrow")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer borrowId;

    @NotNull(message = "bookId est obligatoire")
    Integer bookId;

    @NotNull(message = "userId est obligatoire")
    Integer userId;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    Date issueDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    Date returnDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    Date dueDate;

}
