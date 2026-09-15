package com.ibizabroker.bibliotheque.catalogue.web;

public class BookResponse {

    private Integer bookId;
    private String bookName;
    private String bookAuthor;
    private String bookGenre;
    private Integer noOfCopies;

    public BookResponse() {}

    public BookResponse(Integer bookId, String bookName, String bookAuthor, String bookGenre, Integer noOfCopies) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.bookAuthor = bookAuthor;
        this.bookGenre = bookGenre;
        this.noOfCopies = noOfCopies;
    }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    public String getBookGenre() { return bookGenre; }
    public void setBookGenre(String bookGenre) { this.bookGenre = bookGenre; }

    public Integer getNoOfCopies() { return noOfCopies; }
    public void setNoOfCopies(Integer noOfCopies) { this.noOfCopies = noOfCopies; }
}
