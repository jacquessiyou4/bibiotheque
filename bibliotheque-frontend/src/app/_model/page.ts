/** Page Spring Data telle que sérialisée par le backend (champs utilisés). */
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

/** Taille demandée pour les listes affichées en entier (catalogue, utilisateurs). */
export const LIST_PAGE_SIZE = 1000;
