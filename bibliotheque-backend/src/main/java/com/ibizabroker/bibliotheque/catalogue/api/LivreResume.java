package com.ibizabroker.bibliotheque.catalogue.api;

/** Vue d'un livre exposée aux autres fonctionnalités. */
public final class LivreResume {

    private final Integer bookId;
    private final String bookName;
    private final Integer noOfCopies;

    public LivreResume(Integer bookId, String bookName, Integer noOfCopies) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.noOfCopies = noOfCopies;
    }

    public Integer getBookId() { return bookId; }
    public String getBookName() { return bookName; }
    public Integer getNoOfCopies() { return noOfCopies; }

    /** Au moins un exemplaire en stock. */
    public boolean estDisponible() {
        return noOfCopies != null && noOfCopies >= 1;
    }
}
