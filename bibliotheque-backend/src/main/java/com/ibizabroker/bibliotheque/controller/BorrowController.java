package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.AdherentRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.LivreRepository;
import com.ibizabroker.bibliotheque.entity.Adherent;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Livre;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Tag(name = "Emprunts")
@Repository
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private AdherentRepository adherentRepository;

    @Autowired
    private LivreRepository livreRepository;

    @Operation(summary = "Emprunter un livre", description = "Enregistre l'emprunt d'un exemplaire par un adhérent, décrémente le nombre de copies disponibles et fixe une échéance à 7 jours.")
    @PostMapping
    public String borrowBook(@RequestBody Borrow borrow) {
        Adherent adherent = adherentRepository.findById(borrow.getUserId()).get();
        Livre livre = livreRepository.findById(borrow.getBookId()).get();

        if (livre.getNoOfCopies() < 1) {
            return "The book \"" + livre.getBookName() + "\" is out of stock!";
        }

        livre.borrowBook();
        livreRepository.save(livre);

        Date currentDate = new Date();
        Date overdueDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(overdueDate);
        c.add(Calendar.DATE, 7);
        overdueDate = c.getTime();
        borrow.setIssueDate(currentDate);
        borrow.setDueDate(overdueDate);
        borrowRepository.save(borrow);
        return adherent.getName() + " has borrowed one copy of \"" + livre.getBookName() + "\"!";
    }

    @Operation(summary = "Lister tous les emprunts", description = "Renvoie l'ensemble des emprunts, rendus ou en cours.")
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowRepository.findAll();
    }

    @Operation(summary = "Rendre un livre", description = "Marque un emprunt comme rendu et incrémente à nouveau le nombre de copies disponibles du livre.")
    @PutMapping
    public Borrow returnBook(@RequestBody Borrow borrow) {
        Borrow borrowRecord = borrowRepository.findById(borrow.getBorrowId()).get();
        Livre livre = livreRepository.findById(borrowRecord.getBookId()).get();

        livre.returnBook();
        livreRepository.save(livre);

        Date currentDate = new Date();
        borrowRecord.setReturnDate(currentDate);
        return borrowRepository.save(borrowRecord);
    }

    @Operation(summary = "Emprunts d'un adhérent", description = "Renvoie l'historique des emprunts d'un adhérent donné.")
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(@PathVariable Integer id) {
        return borrowRepository.findByUserId(id);
    }

    @Operation(summary = "Historique d'emprunt d'un livre", description = "Renvoie l'historique des emprunts pour un livre donné.")
    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowRepository.findByBookId(id);
    }

}
