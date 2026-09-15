package com.ibizabroker.bibliotheque.emprunts.api;

import com.ibizabroker.bibliotheque.emprunts.internal.Borrow;

import java.time.LocalDateTime;

/**
 * Emprunt renvoyé par l'API. Les dates sortent en ISO-8601
 * (2026-09-14T10:15:30) : un navigateur ou un autre client les relit sans
 * format maison.
 */
public class BorrowResponse {

    private Integer borrowId;
    private Integer bookId;
    private Integer userId;
    private LocalDateTime issueDate;
    private LocalDateTime returnDate;
    private LocalDateTime dueDate;

    public BorrowResponse() {}

    public BorrowResponse(Integer borrowId, Integer bookId, Integer userId,
                          LocalDateTime issueDate, LocalDateTime returnDate, LocalDateTime dueDate) {
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
    public LocalDateTime getIssueDate() { return issueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public LocalDateTime getDueDate() { return dueDate; }
}
