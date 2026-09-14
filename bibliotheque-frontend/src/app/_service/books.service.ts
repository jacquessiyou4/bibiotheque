import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Books } from '../_model/books';
import { DEFAULT_PAGE_SIZE, LIST_PAGE_SIZE, Page } from '../_model/page';
import { apiUrl } from './api-config';

@Injectable({
  providedIn: 'root'
})
export class BooksService {

  private baseURL = `${apiUrl()}/admin/books`;

  constructor(private httpClient: HttpClient) { }

  // Catalogue complet, pour les écrans qui recoupent ou filtrent tous les
  // livres (emprunt, retour, liste des emprunts). GET /admin/books étant
  // paginé, on demande une page couvrant tout le catalogue.
  getBooksList(): Observable<Books[]> {
    return this.httpClient.get<Page<Books>>(this.baseURL, { params: { size: LIST_PAGE_SIZE } })
      .pipe(map(page => page.content));
  }

  // Tableau paginé de la liste des livres : une seule page à la fois.
  getBooksPage(page: number, size: number = DEFAULT_PAGE_SIZE): Observable<Page<Books>> {
    return this.httpClient.get<Page<Books>>(this.baseURL, { params: { page, size } });
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
