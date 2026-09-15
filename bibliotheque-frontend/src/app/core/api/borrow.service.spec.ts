import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { BorrowService } from './borrow.service';
import { Borrow } from '../../shared/models/borrow';

describe('BorrowService', () => {
  let service: BorrowService;
  let httpMock: HttpTestingController;

  const unEmprunt = (): Borrow => {
    const emprunt = new Borrow();
    emprunt.borrowId = 10;
    emprunt.userId = 1;
    emprunt.bookId = 3;
    return emprunt;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(BorrowService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('borrowBook envoie POST /borrow avec seulement le livre et l’emprunteur', () => {
    service.borrowBook({ bookId: 3, userId: 1 }).subscribe(emprunt => {
      expect(emprunt.borrowId).toBe(10);
    });

    const requete = httpMock.expectOne('http://localhost:8080/api/v1/loans');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual({ bookId: 3, userId: 1 });
    requete.flush(unEmprunt());
  });

  it('returnBook envoie PUT /borrow avec seulement le borrowId', () => {
    service.returnBook(10).subscribe(emprunt => {
      expect(emprunt.borrowId).toBe(10);
    });

    const requete = httpMock.expectOne('http://localhost:8080/api/v1/loans');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ borrowId: 10 });
    requete.flush(unEmprunt());
  });

  it('getBorrowList appelle GET /borrow', () => {
    service.getBorrowList().subscribe(emprunts => {
      expect(emprunts.length).toBe(1);
    });

    httpMock.expectOne('http://localhost:8080/api/v1/loans').flush([unEmprunt()]);
  });

  it('getBooksBorrowedByUser appelle GET /borrow/user/{id}', () => {
    service.getBooksBorrowedByUser(1).subscribe(emprunts => {
      expect(emprunts.length).toBe(1);
    });

    httpMock.expectOne('http://localhost:8080/api/v1/loans/user/1').flush([unEmprunt()]);
  });

  it('getBookBorrowHistory appelle GET /borrow/book/{id}', () => {
    service.getBookBorrowHistory(3).subscribe(emprunts => {
      expect(emprunts.length).toBe(1);
    });

    httpMock.expectOne('http://localhost:8080/api/v1/loans/book/3').flush([unEmprunt()]);
  });
});

