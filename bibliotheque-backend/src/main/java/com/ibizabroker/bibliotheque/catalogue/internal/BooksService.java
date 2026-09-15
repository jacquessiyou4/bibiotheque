package com.ibizabroker.bibliotheque.catalogue.internal;

import com.ibizabroker.bibliotheque.catalogue.api.CatalogueApi;
import com.ibizabroker.bibliotheque.catalogue.api.LivreResume;
import com.ibizabroker.bibliotheque.catalogue.web.BookRequest;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BooksService implements CatalogueApi {

    private final BooksRepository booksRepository;

    public BooksService(BooksRepository booksRepository) {
        this.booksRepository = booksRepository;
    }

    public Page<Books> findAll(Pageable pageable) {
        Page<Books> page = booksRepository.findAll(pageable);
        log.info("Requête GET /api/v1/books — {} livres en base", booksRepository.count());
        return page;
    }

    public Books findById(Integer id) {
        return booksRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BOOK_NOT_FOUND", "Livre avec id " + id + " introuvable."));
    }

    public Books create(BookRequest request) {
        log.info("Création du livre '{}'", request.getBookName());
        Books book = new Books();
        appliquer(request, book);
        return booksRepository.save(book);
    }

    public Books update(Integer id, BookRequest request) {
        log.info("Mise à jour du livre {}", id);
        Books book = findById(id);
        appliquer(request, book);
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

    // ------------------------------------------------------------------
    // CatalogueApi : utilisée par les emprunts et les réservations
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<LivreResume> livre(Integer bookId) {
        return booksRepository.findById(bookId).map(BooksService::resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, LivreResume> livres(Collection<Integer> bookIds) {
        if (bookIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return booksRepository.findAllById(bookIds).stream()
                .map(BooksService::resume)
                .collect(Collectors.toMap(LivreResume::getBookId, Function.identity()));
    }

    @Override
    @Transactional
    public boolean retirerExemplaire(Integer bookId) {
        return booksRepository.decrementerStock(bookId) > 0;
    }

    @Override
    @Transactional
    public void remettreExemplaire(Integer bookId) {
        booksRepository.incrementerStock(bookId);
    }

    private static LivreResume resume(Books book) {
        return new LivreResume(book.getBookId(), book.getBookName(), book.getNoOfCopies());
    }

    private void appliquer(BookRequest request, Books book) {
        book.setBookName(request.getBookName());
        book.setBookAuthor(request.getBookAuthor());
        book.setBookGenre(request.getBookGenre());
        book.setNoOfCopies(request.getNoOfCopies());
    }
}
