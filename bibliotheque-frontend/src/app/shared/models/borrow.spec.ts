import { Borrow } from './borrow';

describe('Borrow', () => {
  it('devrait créer une instance', () => {
    expect(new Borrow()).toBeTruthy();
  });

  it('représente un emprunt en cours avec des dates ISO-8601', () => {
    const emprunt = new Borrow();
    emprunt.borrowId = 10;
    emprunt.userId = 1;
    emprunt.bookId = 3;
    emprunt.issueDate = '2026-09-04T10:00:00';
    emprunt.dueDate = '2026-09-11T10:00:00';
    emprunt.returnDate = null;

    expect(emprunt.borrowId).toBe(10);
    expect(emprunt.returnDate).toBeNull();
    // Contrairement à l'ancien format dd-MM-yyyy, la date se relit.
    expect(new Date(emprunt.dueDate).getDate()).toBe(11);
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
