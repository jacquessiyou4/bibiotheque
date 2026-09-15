package com.ibizabroker.bibliotheque.emprunts.web;

import javax.validation.constraints.NotNull;

/**
 * Demande d'emprunt. Seuls le livre et l'emprunteur viennent du client :
 * identifiant et dates sont fixés par le serveur.
 */
public class BorrowRequest {

    @NotNull(message = "bookId est obligatoire")
    private Integer bookId;

    @NotNull(message = "userId est obligatoire")
    private Integer userId;

    public BorrowRequest() {}

    public BorrowRequest(Integer bookId, Integer userId) {
        this.bookId = bookId;
        this.userId = userId;
    }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
}
