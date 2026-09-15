package com.ibizabroker.bibliotheque.emprunts.internal;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Emprunt persisté. Jamais reçu ni renvoyé tel quel par l'API : les
 * contrôleurs passent par BorrowRequest / ReturnBorrowRequest / BorrowResponse.
 */
@Data
@Entity
@Table(name = "borrow")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer borrowId;

    Integer bookId;

    Integer userId;

    LocalDateTime issueDate;

    LocalDateTime returnDate;

    LocalDateTime dueDate;

}
