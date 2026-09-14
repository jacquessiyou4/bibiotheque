package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BooksService {

    private final BooksRepository booksRepository;

    public BooksService(BooksRepository booksRepository) {
        this.booksRepository = booksRepository;
    }

    public Page<Books> findAll(Pageable pageable) {
        Page<Books> page = booksRepository.findAll(pageable);
        log.info("Requête GET /admin/books — {} livres en base", booksRepository.count());
        return page;
    }

    public Books findById(Integer id) {
        return booksRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Livre avec id " + id + " introuvable."));
    }

    public Books create(Books book) {
        log.info("Création du livre '{}'", book.getBookName());
        return booksRepository.save(book);
    }

    public Books update(Integer id, Books bookDetails) {
        log.info("Mise à jour du livre {}", id);
        Books book = findById(id);
        book.setBookName(bookDetails.getBookName());
        book.setBookAuthor(bookDetails.getBookAuthor());
        book.setBookGenre(bookDetails.getBookGenre());
        book.setNoOfCopies(bookDetails.getNoOfCopies());
        return booksRepository.save(book);
    }

    public void delete(Integer id) {
        log.info("Suppression du livre {}", id);
        Books book = findById(id);
        booksRepository.delete(book);
    }

    public long count() {
        return booksRepository.count();
    }
}
