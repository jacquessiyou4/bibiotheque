import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { BooksService } from './books.service';
import { Books } from '../_model/books';

describe('BooksService', () => {
  let service: BooksService;
  let httpMock: HttpTestingController;

  const unLivre = (): Books => {
    const livre = new Books();
    livre.bookId = 101;
    livre.bookName = 'L1';
    return livre;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(BooksService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('getBooksList appelle GET /admin/books', () => {
    service.getBooksList().subscribe(livres => {
      expect(livres.length).toBe(1);
      expect(livres[0].bookName).toBe('L1');
    });

    httpMock.expectOne('http://localhost:8080/admin/books').flush([unLivre()]);
  });

  it('getBookById appelle GET /admin/books/{id}', () => {
    service.getBookById(101).subscribe(livre => {
      expect(livre.bookId).toBe(101);
    });

    httpMock.expectOne('http://localhost:8080/admin/books/101').flush(unLivre());
  });

    it('createBook envoie POST /admin/books', () => {
    service.createBook(unLivre()).subscribe((livre: any) => {
      expect(livre.bookName).toBe('L1');
    });

    const requete = httpMock.expectOne('http://localhost:8080/admin/books');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body.bookName).toBe('L1');
    requete.flush(unLivre());
  });

    it('updateBook envoie PUT /admin/books/{id}', () => {
    service.updateBook(101, unLivre()).subscribe((livre: any) => {
      expect(livre.bookName).toBe('L1');
    });

    const requete = httpMock.expectOne('http://localhost:8080/admin/books/101');
    expect(requete.request.method).toBe('PUT');
    requete.flush(unLivre());
  });

  it('deleteBook envoie DELETE /admin/books/{id}', () => {
    service.deleteBook(101).subscribe(reponse => {
      expect(reponse).toBeTruthy();
    });

    const requete = httpMock.expectOne('http://localhost:8080/admin/books/101');
    expect(requete.request.method).toBe('DELETE');
    requete.flush({ deleted: true });
  });
});

