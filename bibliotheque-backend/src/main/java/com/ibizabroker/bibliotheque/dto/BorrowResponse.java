package com.ibizabroker.bibliotheque.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.JsonDataSerializer;

import java.util.Date;

/**
 * Emprunt renvoyé par l'API. Mêmes champs JSON que l'entité Borrow (dates
 * au format dd-MM-yyyy) : un champ ajouté plus tard à l'entité ne sort plus
 * automatiquement vers le navigateur.
 */
public class BorrowResponse {

    private Integer borrowId;
    private Integer bookId;
    private Integer userId;

    @JsonSerialize(using = JsonDataSerializer.class)
    private Date issueDate;

    @JsonSerialize(using = JsonDataSerializer.class)
    private Date returnDate;

    @JsonSerialize(using = JsonDataSerializer.class)
    private Date dueDate;

    public BorrowResponse() {}

    public BorrowResponse(Integer borrowId, Integer bookId, Integer userId,
                          Date issueDate, Date returnDate, Date dueDate) {
        this.borrowId = borrowId;
        this.bookId = bookId;
        this.userId = userId;
        this.issueDate = issueDate;
        this.returnDate = returnDate;
        this.dueDate = dueDate;
    }

    public static BorrowResponse from(Borrow borrow) {
        return new BorrowResponse(borrow.getBorrowId(), borrow.getBookId(), borrow.getUserId(),
                borrow.getIssueDate(), borrow.getReturnDate(), borrow.getDueDate());
    }

    public Integer getBorrowId() { return borrowId; }
    public Integer getBookId() { return bookId; }
    public Integer getUserId() { return userId; }
    public Date getIssueDate() { return issueDate; }
    public Date getReturnDate() { return returnDate; }
    public Date getDueDate() { return dueDate; }
}
