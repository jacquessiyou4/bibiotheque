import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../core/auth/auth.guard';
import { BorrowBookComponent } from './borrow-book/borrow-book.component';
import { BorrowListComponent } from './borrow-list/borrow-list.component';
import { ReturnBookComponent } from './return-book/return-book.component';

export const CHEMINS_EMPRUNTS = ['borrow-book', 'return-book', 'borrow-list'];

const routes: Routes = [
  { path: 'borrow-book', component: BorrowBookComponent, canActivate: [AuthGuard], data: { roles: ['User'] } },
  { path: 'return-book', component: ReturnBookComponent, canActivate: [AuthGuard], data: { roles: ['User'] } },
  { path: 'borrow-list', component: BorrowListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
];

/** Emprunts : emprunter, rendre, suivre les prêts. */
@NgModule({
  declarations: [BorrowBookComponent, BorrowListComponent, ReturnBookComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class EmpruntsModule { }
