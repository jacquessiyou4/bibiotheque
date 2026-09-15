// Dates reçues du backend en ISO-8601 (ex. « 2026-09-14T10:15:30 ») :
// le pipe date d'Angular et new Date() savent les relire.
export class Borrow {
    borrowId: number;
    bookId: number;
    userId: number;
    issueDate: string;
    returnDate: string | null;
    dueDate: string;
}

// Corps de POST /borrow : identifiant et dates sont fixés par le serveur.
export interface BorrowRequest {
    bookId: number;
    userId: number;
}
