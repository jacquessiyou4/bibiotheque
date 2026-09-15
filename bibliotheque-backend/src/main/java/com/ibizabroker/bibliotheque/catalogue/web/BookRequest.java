package com.ibizabroker.bibliotheque.catalogue.web;

import com.ibizabroker.bibliotheque.shared.util.Canonical;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Création ou modification d'un livre : l'identifiant vient de l'URL ou de la base. */
public class BookRequest {

    @NotBlank(message = "Le nom du livre est obligatoire")
    @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères")
    private String bookName;

    @NotBlank(message = "L'auteur est obligatoire")
    @Size(max = 255, message = "L'auteur ne peut pas dépasser 255 caractères")
    private String bookAuthor;

    @Size(max = 255, message = "Le genre ne peut pas dépasser 255 caractères")
    private String bookGenre;

    @NotNull(message = "Le nombre de copies est obligatoire")
    @Min(value = 0, message = "Le nombre de copies ne peut pas être négatif")
    private Integer noOfCopies;

    public BookRequest() {}

    public BookRequest(String bookName, String bookAuthor, String bookGenre, Integer noOfCopies) {
        setBookName(bookName);
        setBookAuthor(bookAuthor);
        setBookGenre(bookGenre);
        this.noOfCopies = noOfCopies;
    }

    // Forme canonique : « Victor  Hugo ␠» et « Victor Hugo » désignent le même auteur.
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = Canonical.texte(bookName); }
    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = Canonical.texte(bookAuthor); }
    public String getBookGenre() { return bookGenre; }
    public void setBookGenre(String bookGenre) {
        String genre = Canonical.texte(bookGenre);
        this.bookGenre = genre == null || genre.isEmpty() ? null : genre;
    }
    public Integer getNoOfCopies() { return noOfCopies; }
    public void setNoOfCopies(Integer noOfCopies) { this.noOfCopies = noOfCopies; }
}
