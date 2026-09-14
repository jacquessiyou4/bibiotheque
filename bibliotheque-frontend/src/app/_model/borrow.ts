// Les dates arrivent du backend sous forme de chaînes dd-MM-yyyy
// (JsonDataSerializer).
export class Borrow {
    borrowId: number;
    bookId: number;
    userId: number;
    issueDate: Date | string;
    returnDate: Date | string | null;
    dueDate: Date | string;
}
