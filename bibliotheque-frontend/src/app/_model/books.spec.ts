import { Books } from './books';

describe('Books', () => {
  it('should create an instance', () => {
    expect(new Books()).toBeTruthy();
  });

  it('accepte les champs du formulaire d\u2019administration', () => {
    const livre = new Books();
    livre.bookId = 101;
    livre.bookName = 'Clean Code';
    livre.bookAuthor = 'Robert C. Martin';
    livre.bookGenre = 'Informatique';
    livre.noOfCopies = 3;

    expect(livre.bookName).toBe('Clean Code');
    expect(livre.noOfCopies).toBe(3);
  });

  it('survit à une sérialisation / désérialisation JSON', () => {
    const livre = new Books();
    livre.bookId = 7;
    livre.bookName = 'Domain-Driven Design';
    livre.bookAuthor = 'Eric Evans';
    livre.bookGenre = 'Architecture';
    livre.noOfCopies = 1;

    const copie = JSON.parse(JSON.stringify(livre));

    expect(copie.bookId).toBe(livre.bookId);
    expect(copie.bookName).toBe(livre.bookName);
    expect(copie.bookAuthor).toBe(livre.bookAuthor);
    expect(copie.bookGenre).toBe(livre.bookGenre);
    expect(copie.noOfCopies).toBe(livre.noOfCopies);
  });
});

