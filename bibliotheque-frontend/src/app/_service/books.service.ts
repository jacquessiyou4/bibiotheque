import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Books } from '../_model/books';
import { apiUrl } from './api-config';

@Injectable({
  providedIn: 'root'
})
export class BooksService {

  private baseURL = `${apiUrl()}/admin/books`;

  constructor(private httpClient: HttpClient) { }

  getBooksList(): Observable<Books[]> {
    return this.httpClient.get<Books[]>(`${this.baseURL}`);
  }

  createBook(book: Books): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, book);
  }

  getBookById(bookId: number): Observable<Books> {
    return this.httpClient.get<Books>(`${this.baseURL}/${bookId}`);
  }

  updateBook(bookId: number, book: Books): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${bookId}`, book);
  }

  deleteBook(bookId: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${bookId}`);
  }
}
