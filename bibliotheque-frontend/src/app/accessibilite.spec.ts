import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import * as axe from 'axe-core';

import { LoginComponent } from './features/auth/login/login.component';
import { CreateBookComponent } from './features/catalogue/create-book/create-book.component';
import { BooksListComponent } from './features/catalogue/books-list/books-list.component';
import { TranslatePipe } from './shared/pipes/translate.pipe';
import { TranslationService } from './core/services/translation.service';
import { UsersService } from './core/api/users.service';
import { UserAuthService } from './core/services/user-auth.service';
import { NotificationService } from './core/services/notification.service';
import { BooksService } from './core/api/books.service';

/**
 * Contrôle automatique d'accessibilité (axe-core, règles WCAG 2.1 A et AA) sur
 * les écrans les plus utilisés. Le contraste des couleurs n'est pas vérifié ici :
 * les feuilles de style globales ne sont pas chargées dans les tests unitaires.
 */
describe('Accessibilité (axe-core)', () => {

  const traduction = { translate: (cle: string) => cle, lang$: of('fr'), getLang: () => 'fr' };

  async function violations(fixture: ComponentFixture<unknown>): Promise<string[]> {
    fixture.detectChanges();
    await fixture.whenStable();
    const resultat = await axe.run(fixture.nativeElement as HTMLElement, {
      runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] },
      rules: { 'color-contrast': { enabled: false } }
    });
    return resultat.violations.map(v => `${v.id} (${v.nodes.length}) : ${v.help}`);
  }

  it('formulaire de connexion', async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [LoginComponent, TranslatePipe],
      providers: [
        { provide: TranslationService, useValue: traduction },
        { provide: UsersService, useValue: jasmine.createSpyObj('UsersService', ['login', 'getProfile']) },
        { provide: UserAuthService, useValue: jasmine.createSpyObj('UserAuthService', ['clear']) },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['showError']) },
      ]
    }).compileComponents();

    expect(await violations(TestBed.createComponent(LoginComponent))).toEqual([]);
  });

  it('formulaire de création de livre', async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [CreateBookComponent, TranslatePipe],
      providers: [
        { provide: TranslationService, useValue: traduction },
        { provide: BooksService, useValue: jasmine.createSpyObj('BooksService', ['createBook']) },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['showError']) },
      ]
    }).compileComponents();

    expect(await violations(TestBed.createComponent(CreateBookComponent))).toEqual([]);
  });

  it('liste des livres', async () => {
    const booksService = jasmine.createSpyObj('BooksService', ['getBooksPage', 'deleteBook']);
    booksService.getBooksPage.and.returnValue(of({
      content: [{ bookId: 1, bookName: 'Le Petit Prince', bookAuthor: 'Saint-Exupéry', bookGenre: 'Conte', noOfCopies: 3 }],
      totalElements: 1, totalPages: 1, number: 0, size: 10
    }));
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [BooksListComponent, TranslatePipe],
      providers: [
        { provide: TranslationService, useValue: traduction },
        { provide: BooksService, useValue: booksService },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['showError', 'showSuccess']) },
      ]
    }).compileComponents();

    expect(await violations(TestBed.createComponent(BooksListComponent))).toEqual([]);
  });
});
