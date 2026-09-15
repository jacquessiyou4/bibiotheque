import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../core/auth/auth.guard';
import { BooksListComponent } from './books-list/books-list.component';
import { BookDetailsComponent } from './book-details/book-details.component';
import { CreateBookComponent } from './create-book/create-book.component';
import { UpdateBookComponent } from './update-book/update-book.component';

export const CHEMINS_CATALOGUE = ['books', 'create-book', 'update-book', 'book-details'];

const routes: Routes = [
  { path: 'books', component: BooksListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'create-book', component: CreateBookComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'update-book/:bookId', component: UpdateBookComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'book-details/:bookId', component: BookDetailsComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
];

/** Catalogue : écrans bibliothécaire de gestion des livres. */
@NgModule({
  declarations: [BooksListComponent, BookDetailsComponent, CreateBookComponent, UpdateBookComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CatalogueModule { }
