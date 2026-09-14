package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Entity
@Table(name = "books")
public class Books {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer bookId;

    @NotBlank(message = "Le nom du livre est obligatoire")
    @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères")
    String bookName;

    @NotBlank(message = "L'auteur est obligatoire")
    String bookAuthor;

    String bookGenre;

    @Min(value = 0, message = "Le nombre de copies ne peut pas être négatif")
    Integer noOfCopies;

    public void borrowBook() {
        this.noOfCopies--;
    }

    public void returnBook() {
        this.noOfCopies++;
    }

}
