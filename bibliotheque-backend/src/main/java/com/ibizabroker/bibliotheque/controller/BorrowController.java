package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/borrow")
public class BorrowController {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PostMapping
    public String borrowBook(@RequestBody Borrow borrow) {
        Users user = usersRepository.findById(borrow.getUserId())
            .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        Books book = booksRepository.findById(borrow.getBookId())
            .orElseThrow(() -> new NotFoundException("Livre introuvable"));

        if (book.getNoOfCopies() < 1) {
            return "The book \"" + book.getBookName() + "\" is out of stock!";
        }

        book.borrowBook();
        booksRepository.save(book);

        Date currentDate = new Date();
        Date overdueDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(overdueDate);
        c.add(Calendar.DATE, 7);
        overdueDate = c.getTime();
        borrow.setIssueDate(currentDate);
        borrow.setDueDate(overdueDate);
        borrowRepository.save(borrow);
        return user.getName() + " has borrowed one copy of \"" + book.getBookName() + "\"!";
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PutMapping
    public Borrow returnBook(@RequestBody Borrow borrow) {
        Borrow borrowBook = borrowRepository.findById(borrow.getBorrowId())
            .orElseThrow(() -> new NotFoundException("Emprunt introuvable"));
        Books book = booksRepository.findById(borrowBook.getBookId())
            .orElseThrow(() -> new NotFoundException("Livre introuvable"));

        book.returnBook();
        booksRepository.save(book);

        Date currentDate = new Date();
        borrowBook.setReturnDate(currentDate);
        return borrowRepository.save(borrowBook);
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(@PathVariable Integer id) {
        return borrowRepository.findByUserId(id);
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowRepository.findByBookId(id);
    }

}
