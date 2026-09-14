import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { CreateBookComponent } from './create-book.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { BooksService } from '../_service/books.service';
import { NotificationService } from '../_service/notification.service';

describe('CreateBookComponent', () => {
  let component: CreateBookComponent;
  let fixture: ComponentFixture<CreateBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['createBook']);
    notificationSpy = jasmine.createSpyObj('NotificationService', ['showError']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [ CreateBookComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: NotificationService, useValue: notificationSpy },
      ]
    })
    .compileComponents();

    booksServiceSpy.createBook.and.returnValue(of({ bookId: 201 }));

    fixture = TestBed.createComponent(CreateBookComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('onSubmit crée le livre saisi puis retourne à la liste', () => {
    component.book.bookName = 'Nouveau Livre';
    component.book.noOfCopies = 3;
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(booksServiceSpy.createBook).toHaveBeenCalledWith(component.book);
    expect(router.navigate).toHaveBeenCalledWith(['/books']);
  });

  it('saveBook transmet le modèle complet au service', () => {
    component.book.bookName = 'Titre';
    component.book.bookAuthor = 'Auteur';
    component.book.bookGenre = 'Roman';
    component.book.noOfCopies = 1;
    // saveBook navigue vers /books en cas de succès : sans stub, la navigation
    // échoue (aucune route de test) et fait échouer Karma en afterAll.
    spyOn(router, 'navigate');

    component.saveBook();

    const livreEnvoyé = booksServiceSpy.createBook.calls.mostRecent().args[0];
    expect(livreEnvoyé.bookAuthor).toBe('Auteur');
    expect(livreEnvoyé.bookGenre).toBe('Roman');
    expect(livreEnvoyé.noOfCopies).toBe(1);
  });

  it('onSubmit signale l’échec de la création et reste sur le formulaire', () => {
    booksServiceSpy.createBook.and.returnValue(throwError(() => new Error('400')));
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(notificationSpy.showError).toHaveBeenCalledWith('Erreur lors de la création du livre');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
