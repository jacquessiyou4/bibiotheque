-- Readjuste tous les ids de toutes les tables pour repartir de 1.
-- A executer backend ARRETE (le scheduler d'expiration des reservations
-- tourne toutes les 60s et pourrait interferer sinon).

\set ON_ERROR_STOP on

BEGIN;

-- 1. Retirer temporairement les FK de reservation vers livre/adherent
--    (elles seront reposees a la fin, une fois les ids stabilises).
ALTER TABLE reservation DROP CONSTRAINT IF EXISTS reservation_book_id_fkey;
ALTER TABLE reservation DROP CONSTRAINT IF EXISTS reservation_user_id_fkey;

-- 2. livre : 121-125 -> 1-5 (plages disjointes, pas de collision possible)
WITH mapping AS (
    SELECT book_id AS old_id, ROW_NUMBER() OVER (ORDER BY book_id) AS new_id
    FROM livre
)
UPDATE reservation r SET book_id = m.new_id
FROM mapping m WHERE r.book_id = m.old_id;

WITH mapping AS (
    SELECT book_id AS old_id, ROW_NUMBER() OVER (ORDER BY book_id) AS new_id
    FROM livre
)
UPDATE borrow b SET book_id = m.new_id
FROM mapping m WHERE b.book_id = m.old_id;

WITH mapping AS (
    SELECT book_id AS old_id, ROW_NUMBER() OVER (ORDER BY book_id) AS new_id
    FROM livre
)
UPDATE livre l SET book_id = m.new_id
FROM mapping m WHERE l.book_id = m.old_id;

-- 3. adherent : 126-128 -> 1-3
WITH mapping AS (
    SELECT user_id AS old_id, ROW_NUMBER() OVER (ORDER BY user_id) AS new_id
    FROM adherent
)
UPDATE reservation r SET user_id = m.new_id
FROM mapping m WHERE r.user_id = m.old_id;

WITH mapping AS (
    SELECT user_id AS old_id, ROW_NUMBER() OVER (ORDER BY user_id) AS new_id
    FROM adherent
)
UPDATE borrow b SET user_id = m.new_id
FROM mapping m WHERE b.user_id = m.old_id;

WITH mapping AS (
    SELECT user_id AS old_id, ROW_NUMBER() OVER (ORDER BY user_id) AS new_id
    FROM adherent
)
UPDATE adherent a SET user_id = m.new_id
FROM mapping m WHERE a.user_id = m.old_id;

-- 4. administrator : deja a 1, mais on applique la meme logique pour rester generique
WITH mapping AS (
    SELECT user_id AS old_id, ROW_NUMBER() OVER (ORDER BY user_id) AS new_id
    FROM administrator
)
UPDATE administrator a SET user_id = m.new_id
FROM mapping m WHERE a.user_id = m.old_id AND a.user_id <> m.new_id;

-- 5. reservation : ses propres ids (colonne "id"), independants des FK deja reposees ci-dessus
WITH mapping AS (
    SELECT id AS old_id, ROW_NUMBER() OVER (ORDER BY id) AS new_id
    FROM reservation
)
UPDATE reservation r SET id = m.new_id
FROM mapping m WHERE r.id = m.old_id;

-- 6. borrow : ses propres ids (colonne "borrow_id")
WITH mapping AS (
    SELECT borrow_id AS old_id, ROW_NUMBER() OVER (ORDER BY borrow_id) AS new_id
    FROM borrow
)
UPDATE borrow b SET borrow_id = m.new_id
FROM mapping m WHERE b.borrow_id = m.old_id;

-- 7. pictures : deja a 1, meme logique generique
WITH mapping AS (
    SELECT id AS old_id, ROW_NUMBER() OVER (ORDER BY id) AS new_id
    FROM pictures
)
UPDATE pictures p SET id = m.new_id
FROM mapping m WHERE p.id = m.old_id AND p.id <> m.new_id;

-- 8. Reposer les FK de reservation
ALTER TABLE reservation ADD CONSTRAINT reservation_book_id_fkey FOREIGN KEY (book_id) REFERENCES livre(book_id);
ALTER TABLE reservation ADD CONSTRAINT reservation_user_id_fkey FOREIGN KEY (user_id) REFERENCES adherent(user_id);

-- 9. Recaler chaque sequence IDENTITY/SERIAL juste au-dessus du nouveau max.
DO $$
DECLARE seqname text;
BEGIN
    seqname := pg_get_serial_sequence('livre', 'book_id');
    PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(book_id), 0) FROM livre), 0) + 1, false);

    seqname := pg_get_serial_sequence('adherent', 'user_id');
    PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(user_id), 0) FROM adherent), 0) + 1, false);

    seqname := pg_get_serial_sequence('administrator', 'user_id');
    PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(user_id), 0) FROM administrator), 0) + 1, false);

    seqname := pg_get_serial_sequence('reservation', 'id');
    PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(id), 0) FROM reservation), 0) + 1, false);

    seqname := pg_get_serial_sequence('borrow', 'borrow_id');
    PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(borrow_id), 0) FROM borrow), 0) + 1, false);

    seqname := pg_get_serial_sequence('pictures', 'id');
    IF seqname IS NOT NULL THEN
        PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(id), 0) FROM pictures), 0) + 1, false);
    END IF;
END $$;

COMMIT;

-- 10. Verification
SELECT 'administrator' t, user_id::text id FROM administrator
UNION ALL SELECT 'adherent', user_id::text FROM adherent
UNION ALL SELECT 'livre', book_id::text FROM livre
UNION ALL SELECT 'reservation', id::text FROM reservation
UNION ALL SELECT 'borrow', borrow_id::text FROM borrow
UNION ALL SELECT 'pictures', id::text FROM pictures
ORDER BY t, id;

SELECT r.id, r.book_id, l.book_name, r.user_id, a.username, r.statut
FROM reservation r JOIN livre l ON l.book_id=r.book_id JOIN adherent a ON a.user_id=r.user_id
ORDER BY r.id;

SELECT b.borrow_id, b.book_id, l.book_name, b.user_id, a.username
FROM borrow b JOIN livre l ON l.book_id=b.book_id JOIN adherent a ON a.user_id=b.user_id
ORDER BY b.borrow_id;
