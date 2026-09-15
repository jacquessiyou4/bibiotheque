# 0004 — Glossaire et règles de nommage

- **Statut** : Acceptée
- **Date** : 15 septembre 2026

## Contexte

Le code mélange deux langues pour les mêmes notions : `Books`, `Borrow` et
`Users` côté catalogue et emprunts ; `livre`, `adherent`, `statut` et
`/annuler` côté réservations. Les noms d'entités sont tantôt au pluriel
(`Books`), tantôt au singulier (`Borrow`). Seules les réservations sont sous
`/api`.

## Décision

1. **Code, API et schéma en anglais** ; **interface en français et en anglais**
   par la traduction (`translations.ts`).
2. Les termes métier suivent ce glossaire :

   | Français (interface) | Code et API | Table / colonne |
   |---|---|---|
   | livre | `Book` | `book`, `book_id` |
   | exemplaires disponibles | `availableCopies` | `available_copies` |
   | adhérent | `Member` (rôle `ADHERENT` conservé) | `user_id` |
   | bibliothécaire | `Librarian` (rôle `BIBLIOTHECAIRE` conservé) | — |
   | emprunt | `Loan` | `loan`, `loan_id` |
   | date limite de retour | `dueDate` | `due_date` |
   | réservation | `Reservation` | `reservation` |
   | annuler une réservation | `status = CANCELLED` | `status` |
   | expirée | `EXPIRED` | — |

3. Entités **au singulier**, tables en `snake_case` au singulier, URL au pluriel
   (`/api/v1/books`).
4. Les renommages se font **au passage** : quand une classe est déplacée dans
   son package de fonctionnalité (ADR 0003), jamais en une passe globale.
   Chaque renommage de colonne est une nouvelle migration, et l'ancien endpoint
   reste disponible une version, marqué obsolète.

## Conséquences

- Le vocabulaire reste stable pour les nouveaux développeurs et les clients de
  l'API.
- Pendant la transition, les deux vocabulaires coexistent ; ce glossaire fait
  foi.
- Les noms de rôles Keycloak (`ADHERENT`, `BIBLIOTHECAIRE`) ne changent pas :
  ils figurent dans les jetons déjà émis et dans la configuration du realm.
