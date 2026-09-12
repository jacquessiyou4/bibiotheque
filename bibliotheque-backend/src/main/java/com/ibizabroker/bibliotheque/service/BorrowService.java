package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BadRequestException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class BorrowService {

    private final BorrowRepository borrowRepository;
    private final UsersRepository usersRepository;
    private final BooksRepository booksRepository;

    public BorrowService(BorrowRepository borrowRepository, UsersRepository usersRepository, BooksRepository booksRepository) {
        this.borrowRepository = borrowRepository;
        this.usersRepository = usersRepository;
        this.booksRepository = booksRepository;
    }

    @Transactional
    public String borrowBook(Borrow borrow) {
        Users user = usersRepository.findById(borrow.getUserId())
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        Books book = booksRepository.findById(borrow.getBookId())
                .orElseThrow(() -> new NotFoundException("Livre introuvable"));

        if (book.getNoOfCopies() < 1) {
            throw new BadRequestException("Le livre \"" + book.getBookName() + "\" n'est plus disponible.");
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

        log.info("Emprunt: {} a emprunté une copie de \"{}\"", user.getName(), book.getBookName());
        return user.getName() + " a emprunté une copie de \"" + book.getBookName() + "\" !";
    }

    public List<Borrow> findAll() {
        return borrowRepository.findAll();
    }

    @Transactional
    public Borrow returnBook(Borrow borrow) {
        Borrow borrowBook = borrowRepository.findById(borrow.getBorrowId())
                .orElseThrow(() -> new NotFoundException("Emprunt introuvable"));
        Books book = booksRepository.findById(borrowBook.getBookId())
                .orElseThrow(() -> new NotFoundException("Livre introuvable"));

        book.returnBook();
        booksRepository.save(book);

        Date currentDate = new Date();
        borrowBook.setReturnDate(currentDate);

        log.info("Retour: emprunt {} rendu pour \"{}\"", borrowBook.getBorrowId(), book.getBookName());
        return borrowRepository.save(borrowBook);
    }

    public List<Borrow> findByUserId(Integer userId) {
        return borrowRepository.findByUserId(userId);
    }

    public List<Borrow> findByBookId(Integer bookId) {
        return borrowRepository.findByBookId(bookId);
    }
}
