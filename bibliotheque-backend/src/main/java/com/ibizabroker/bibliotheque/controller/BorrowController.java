package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.service.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PostMapping
    public String borrowBook(@RequestBody Borrow borrow) {
        return borrowService.borrowBook(borrow);
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowService.findAll();
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PutMapping
    public Borrow returnBook(@RequestBody Borrow borrow) {
        return borrowService.returnBook(borrow);
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(@PathVariable Integer id) {
        return borrowService.findByUserId(id);
    }

    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowService.findByBookId(id);
    }

}
