import { Borrow } from './borrow';

describe('Borrow', () => {
  it('devrait créer une instance', () => {
    expect(new Borrow()).toBeTruthy();
  });

  it('représente un emprunt en cours avec issueDate et dueDate', () => {
    const emprunt = new Borrow();
    emprunt.borrowId = 10;
    emprunt.userId = 1;
    emprunt.bookId = 3;
    emprunt.issueDate = new Date(2026, 8, 4);
    emprunt.dueDate = new Date(2026, 8, 11);
        emprunt.returnDate = null as unknown as Date;

    expect(emprunt.borrowId).toBe(10);
    expect(emprunt.returnDate).toBeNull();
  });

  it('survit à une sérialisation / désérialisation JSON', () => {
    const emprunt = new Borrow();
    emprunt.borrowId = 10;
    emprunt.userId = 1;
    emprunt.bookId = 3;

    const copie = JSON.parse(JSON.stringify(emprunt)) as Borrow;

    expect(copie.borrowId).toBe(10);
    expect(copie.bookId).toBe(3);
    expect(copie.userId).toBe(1);
  });
});

