/** Page Spring Data telle que sérialisée par le backend (champs utilisés). */
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

/**
 * Taille demandée quand une liste complète est nécessaire (menus de
 * sélection, recoupements entre livres, utilisateurs et emprunts).
 */
export const LIST_PAGE_SIZE = 1000;

/** Taille d'une page dans les tableaux paginés (livres, utilisateurs). */
export const DEFAULT_PAGE_SIZE = 10;
