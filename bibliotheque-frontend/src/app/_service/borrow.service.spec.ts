import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { BorrowService } from './borrow.service';
import { Borrow } from '../_model/borrow';

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

  it('borrowBook envoie POST /borrow', () => {
    service.borrowBook(unEmprunt()).subscribe(message => {
      expect(message).toBeTruthy();
    });

    const requete = httpMock.expectOne('http://localhost:8080/borrow');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body.bookId).toBe(3);
    requete.flush('message');
  });

    it('returnBook envoie PUT /borrow', () => {
    service.returnBook(unEmprunt()).subscribe((emprunt: any) => {
      expect(emprunt.borrowId).toBe(10);
    });

    const requete = httpMock.expectOne('http://localhost:8080/borrow');
    expect(requete.request.method).toBe('PUT');
    requete.flush(unEmprunt());
  });

  it('getBorrowList appelle GET /borrow', () => {
    service.getBorrowList().subscribe(emprunts => {
      expect(emprunts.length).toBe(1);
    });

    httpMock.expectOne('http://localhost:8080/borrow').flush([unEmprunt()]);
  });

  it('getBooksBorrowedByUser appelle GET /borrow/user/{id}', () => {
    service.getBooksBorrowedByUser(1).subscribe(emprunts => {
      expect(emprunts.length).toBe(1);
    });

    httpMock.expectOne('http://localhost:8080/borrow/user/1').flush([unEmprunt()]);
  });

  it('getBookBorrowHistory appelle GET /borrow/book/{id}', () => {
    service.getBookBorrowHistory(3).subscribe(emprunts => {
      expect(emprunts.length).toBe(1);
    });

    httpMock.expectOne('http://localhost:8080/borrow/book/3').flush([unEmprunt()]);
  });
});

