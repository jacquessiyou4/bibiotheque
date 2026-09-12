package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Livres", description = "CRUD des livres (admin)")
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin")
@Slf4j
public class BooksController {

    @Autowired
    private BooksRepository booksRepository;

    @Operation(summary = "Lister les livres (pagination)")
    @GetMapping("/books")
    public Page<Books> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bookId") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Books> booksPage = booksRepository.findAll(pageable);
        log.info("Requête GET /admin/books — {} livres en base", booksRepository.count());
        return booksPage;
    }

    @Operation(summary = "Obtenir un livre par son identifiant")
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/books/{id}")
    public ResponseEntity<Books> getBookById(@PathVariable Integer id) {
        log.info("Requête GET /admin/books/{}", id);
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));
        return ResponseEntity.ok(book);
    }

    @Operation(summary = "Créer un nouveau livre")
    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/books")
    public Books createBook(@Valid @RequestBody Books book) {
        log.info("Requête POST /admin/books — création du livre '{}'", book.getBookName());
        return booksRepository.save(book);
    }

    @Operation(summary = "Modifier un livre existant")
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/books/{id}")
    public ResponseEntity<Books> updateBook(@PathVariable Integer id, @Valid @RequestBody Books bookDetails) {
        log.info("Requête PUT /admin/books/{}", id);
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));

        book.setBookName(bookDetails.getBookName());
        book.setBookAuthor(bookDetails.getBookAuthor());
        book.setBookGenre(bookDetails.getBookGenre());
        book.setNoOfCopies(bookDetails.getNoOfCopies());

        Books updatedBook = booksRepository.save(book);
        return ResponseEntity.ok(updatedBook);
    }

    @Operation(summary = "Supprimer un livre")
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteBook(@PathVariable Integer id) {
        log.info("Requête DELETE /admin/books/{}", id);
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));

        booksRepository.delete(book);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }
}
