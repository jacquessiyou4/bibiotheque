import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Borrow, BorrowRequest } from '../../shared/models/borrow';
import { apiUrl } from '../services/api-config';

@Injectable({
  providedIn: 'root'
})
export class BorrowService {

  private baseURL = `${apiUrl()}/api/v1/loans`;

  constructor(private httpClient: HttpClient) { }

  getBorrowList(): Observable<Borrow[]> {
    return this.httpClient.get<Borrow[]>(`${this.baseURL}`);
  }

  borrowBook(request: BorrowRequest): Observable<Borrow> {
    return this.httpClient.post<Borrow>(`${this.baseURL}`, { bookId: request.bookId, userId: request.userId });
  }

  // Le backend n'attend que l'emprunt concerné : livre et emprunteur sont lus en base.
  returnBook(borrowId: number): Observable<Borrow> {
    return this.httpClient.put<Borrow>(`${this.baseURL}`, { borrowId });
  }

  getBooksBorrowedByUser(userId: number): Observable<Borrow[]> {
    return this.httpClient.get<Borrow[]>(`${this.baseURL}/user/${userId}`);
  }

  getBookBorrowHistory(bookId: number): Observable<Borrow[]> {
    return this.httpClient.get<Borrow[]>(`${this.baseURL}/book/${bookId}`);
  }
}
