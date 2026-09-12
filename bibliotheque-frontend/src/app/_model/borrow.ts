export class Borrow {
    borrowId: number;
    bookId: number;
    userId: number;
    issueDate: Date;
    returnDate: Date | null;
    dueDate: Date;
}
