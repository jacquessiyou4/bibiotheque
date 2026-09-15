-- V4 : contraintes d'intégrité, verrouillage optimiste et index
--
-- Jusqu'ici ces règles n'étaient vérifiées que par le code Java : deux
-- requêtes simultanées pouvaient les contourner (même username en double,
-- stock négatif, deux réservations actives identiques). La base les garantit
-- désormais. PostgreSQL exécute la migration dans une transaction : si une
-- vérification échoue, rien n'est modifié.

-- ---------------------------------------------------------------------------
-- 1. Vérifications préalables : un message clair plutôt qu'un échec obscur
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT lower(username) FROM users GROUP BY lower(username) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'V4 : usernames en double (sans tenir compte de la casse). '
            'Fusionnez-les avant de relancer : SELECT lower(username), count(*) FROM users GROUP BY 1 HAVING count(*) > 1;';
    END IF;
    IF EXISTS (SELECT 1 FROM books WHERE no_of_copies < 0) THEN
        RAISE EXCEPTION 'V4 : livres au stock négatif. Corrigez-les : SELECT * FROM books WHERE no_of_copies < 0;';
    END IF;
    IF EXISTS (SELECT 1 FROM borrow b LEFT JOIN books l ON l.book_id = b.book_id WHERE l.book_id IS NULL)
       OR EXISTS (SELECT 1 FROM borrow b LEFT JOIN users u ON u.user_id = b.user_id WHERE u.user_id IS NULL) THEN
        RAISE EXCEPTION 'V4 : emprunts orphelins (livre ou utilisateur supprimé). '
            'Listez-les : SELECT * FROM borrow WHERE book_id NOT IN (SELECT book_id FROM books) OR user_id NOT IN (SELECT user_id FROM users);';
    END IF;
    IF EXISTS (SELECT livre_id, adherent_id FROM reservation
               WHERE statut IN ('EN_ATTENTE', 'DISPONIBLE')
               GROUP BY livre_id, adherent_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'V4 : réservations actives en double pour un même livre et adhérent. '
            'Annulez les doublons (statut ANNULEE) avant de relancer.';
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Utilisateurs : un username unique, casse ignorée (Keycloak le met en minuscules)
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_users_username_lower ON users (lower(username));

-- ---------------------------------------------------------------------------
-- 3. Livres : stock obligatoire et jamais négatif, version pour le verrouillage optimiste
-- ---------------------------------------------------------------------------
UPDATE books SET no_of_copies = 0 WHERE no_of_copies IS NULL;
ALTER TABLE books
    ALTER COLUMN no_of_copies SET NOT NULL,
    ADD CONSTRAINT ck_books_no_of_copies_positif CHECK (no_of_copies >= 0),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 4. Emprunts : même politique que les réservations (clés étrangères)
-- ---------------------------------------------------------------------------
ALTER TABLE borrow
    ALTER COLUMN book_id SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books (book_id),
    ADD CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- ---------------------------------------------------------------------------
-- 5. Réservations : une seule réservation active par livre et adhérent (RG-02),
--    version pour le verrouillage optimiste
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_reservation_active_livre_adherent
    ON reservation (livre_id, adherent_id)
    WHERE statut IN ('EN_ATTENTE', 'DISPONIBLE');

ALTER TABLE reservation ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 6. Index des recherches fréquentes (voir BEST_PRACTICES.md, recommandation 81)
-- ---------------------------------------------------------------------------
CREATE INDEX ix_borrow_user_id ON borrow (user_id);
CREATE INDEX ix_borrow_book_id ON borrow (book_id);
CREATE INDEX ix_reservation_adherent_id ON reservation (adherent_id);
CREATE INDEX ix_reservation_statut_expiration ON reservation (statut, date_expiration);
