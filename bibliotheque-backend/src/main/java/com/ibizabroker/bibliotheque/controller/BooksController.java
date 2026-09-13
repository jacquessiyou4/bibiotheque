package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.BookResponse;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.service.BooksService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Livres", description = "CRUD des livres (admin)")
@RestController
@RequestMapping("/admin")
@Slf4j
public class BooksController {

    private final BooksService booksService;

    public BooksController(BooksService booksService) {
        this.booksService = booksService;
    }

    @Operation(summary = "Lister les livres (pagination)")
    @GetMapping("/books")
    public Page<BookResponse> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bookId") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return booksService.findAll(pageable).map(this::toBookResponse);
    }

    @Operation(summary = "Obtenir un livre par son identifiant")
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/books/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Integer id) {
        log.info("Requête GET /admin/books/{}", id);
        return ResponseEntity.ok(toBookResponse(booksService.findById(id)));
    }

    @Operation(summary = "Créer un nouveau livre")
    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/books")
    public BookResponse createBook(@Valid @RequestBody Books book) {
        return toBookResponse(booksService.create(book));
    }

    @Operation(summary = "Modifier un livre existant")
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/books/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Integer id, @Valid @RequestBody Books bookDetails) {
        return ResponseEntity.ok(toBookResponse(booksService.update(id, bookDetails)));
    }

    @Operation(summary = "Supprimer un livre")
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteBook(@PathVariable Integer id) {
        booksService.delete(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    private BookResponse toBookResponse(Books book) {
        return new BookResponse(
            book.getBookId(),
            book.getBookName(),
            book.getBookAuthor(),
            book.getBookGenre(),
            book.getNoOfCopies()
        );
    }
}
