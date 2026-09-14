import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Books } from '../_model/books';
import { LIST_PAGE_SIZE, Page } from '../_model/page';
import { apiUrl } from './api-config';

@Injectable({
  providedIn: 'root'
})
export class BooksService {

  private baseURL = `${apiUrl()}/admin/books`;

  constructor(private httpClient: HttpClient) { }

  // GET /admin/books est paginé côté backend (Page<BookResponse>) : on
  // demande une page couvrant tout le catalogue et on n'expose que son
  // contenu, comme attendu par les composants.
  getBooksList(): Observable<Books[]> {
    return this.httpClient.get<Page<Books>>(this.baseURL, { params: { size: LIST_PAGE_SIZE } })
      .pipe(map(page => page.content));
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
