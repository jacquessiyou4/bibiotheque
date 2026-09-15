package com.ibizabroker.bibliotheque.emprunts.web;

import javax.validation.constraints.NotNull;

/** Retour d'un livre : l'emprunt concerné suffit, le reste est lu en base. */
public class ReturnBorrowRequest {

    @NotNull(message = "borrowId est obligatoire")
    private Integer borrowId;

    public ReturnBorrowRequest() {}

    public ReturnBorrowRequest(Integer borrowId) {
        this.borrowId = borrowId;
    }

    public Integer getBorrowId() { return borrowId; }
    public void setBorrowId(Integer borrowId) { this.borrowId = borrowId; }
}
