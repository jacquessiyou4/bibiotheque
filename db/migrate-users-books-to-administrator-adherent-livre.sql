-- Migration : Users/Books (role via table Role/USER_ROLE) -> Administrator/Adherent/Livre
-- (le type Java discrimine desormais le role, plus de table Role).
-- A executer backend ARRETE, AVANT de le redemarrer.

\set ON_ERROR_STOP on

BEGIN;

-- Garde-fou : refuse de rejouer la migration si elle a deja ete appliquee.
DO $$
BEGIN
    IF to_regclass('public.users') IS NULL OR to_regclass('public.books') IS NULL THEN
        RAISE EXCEPTION 'Table users ou books absente : la migration a probablement deja ete appliquee. Arret par securite, aucune modification effectuee.';
    END IF;
END $$;

-- 1. Creer les nouvelles tables, avec exactement la forme que Hibernate
--    generera pour les entites Administrator/Adherent/Livre
--    (GenerationType.IDENTITY sur PostgreSQLDialect => colonne SERIAL,
--    meme mecanisme que reservation.id / borrow.borrow_id existants).
CREATE TABLE administrator (
    user_id  SERIAL PRIMARY KEY,
    username VARCHAR(255),
    name     VARCHAR(255),
    password VARCHAR(255),
    matricule VARCHAR(255)
);

CREATE TABLE adherent (
    user_id  SERIAL PRIMARY KEY,
    username VARCHAR(255),
    name     VARCHAR(255),
    password VARCHAR(255),
    matricule VARCHAR(255)
);

CREATE TABLE livre (
    book_id      SERIAL PRIMARY KEY,
    book_name    VARCHAR(255),
    book_author  VARCHAR(255),
    book_genre   VARCHAR(255),
    no_of_copies INTEGER,
    matricule    VARCHAR(255)
);

-- 2. Copier les donnees, en conservant les identifiants d'origine
--    (reservation.book_id/user_id et borrow.book_id/user_id pointent
--    deja sur ces valeurs numeriques).

-- 2a. Livre <- copie integrale de books (deja nettoye : uniquement L1-L5)
INSERT INTO livre (book_id, book_name, book_author, book_genre, no_of_copies, matricule)
SELECT book_id, book_name, book_author, book_genre, no_of_copies, matricule
FROM books;

-- 2b. Administrator <- users dont le role (via user_role/role) est 'Admin'
INSERT INTO administrator (user_id, username, name, password, matricule)
SELECT u.user_id, u.username, u.name, u.password, u.matricule
FROM users u
JOIN user_role ur ON ur.user_id = u.user_id
JOIN role r ON r.role_id = ur.role_id
WHERE r.role_name = 'Admin';

-- 2c. Adherent <- users dont le role (via user_role/role) est 'User'
INSERT INTO adherent (user_id, username, name, password, matricule)
SELECT u.user_id, u.username, u.name, u.password, u.matricule
FROM users u
JOIN user_role ur ON ur.user_id = u.user_id
JOIN role r ON r.role_id = ur.role_id
WHERE r.role_name = 'User';

-- 3. Avancer les sequences IDENTITY des nouvelles tables au-dela des
--    identifiants poses a la main (les sequences liees a une colonne SERIAL
--    ne s'avancent PAS automatiquement lors d'un INSERT avec valeur explicite).
DO $$
DECLARE seqname text;
BEGIN
    seqname := pg_get_serial_sequence('livre', 'book_id');
    IF seqname IS NOT NULL THEN
        PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(book_id), 1) FROM livre), 1));
    END IF;

    seqname := pg_get_serial_sequence('administrator', 'user_id');
    IF seqname IS NOT NULL THEN
        PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(user_id), 1) FROM administrator), 1));
    END IF;

    seqname := pg_get_serial_sequence('adherent', 'user_id');
    IF seqname IS NOT NULL THEN
        PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(user_id), 1) FROM adherent), 1));
    END IF;
END $$;

-- 4. Garde-fous : verifier qu'aucune reservation n'est orpheline avant de
--    reposer les FK (le nettoyage prealable a deja retire les lignes qui
--    l'auraient ete).
DO $$
DECLARE nb_orphelines integer;
BEGIN
    SELECT count(*) INTO nb_orphelines
    FROM reservation r
    WHERE NOT EXISTS (SELECT 1 FROM adherent a WHERE a.user_id = r.user_id);

    IF nb_orphelines > 0 THEN
        RAISE EXCEPTION 'Incoherence inattendue : % reservation(s) referencent un user_id absent de adherent.', nb_orphelines;
    END IF;
END $$;

DO $$
DECLARE nb_orphelines integer;
BEGIN
    SELECT count(*) INTO nb_orphelines
    FROM reservation r
    WHERE NOT EXISTS (SELECT 1 FROM livre l WHERE l.book_id = r.book_id);

    IF nb_orphelines > 0 THEN
        RAISE EXCEPTION 'Incoherence inattendue : % reservation(s) referencent un book_id absent de livre.', nb_orphelines;
    END IF;
END $$;

-- 5. Reposer les FK de reservation sur les nouvelles tables.
ALTER TABLE reservation DROP CONSTRAINT fks25sh1gv4uidcd1c1qjux3af2; -- book_id -> books
ALTER TABLE reservation DROP CONSTRAINT fkrea93581tgkq61mdl13hehami; -- user_id -> users

ALTER TABLE reservation ADD FOREIGN KEY (book_id) REFERENCES livre(book_id);
ALTER TABLE reservation ADD FOREIGN KEY (user_id) REFERENCES adherent(user_id);

-- 6. Supprimer les anciennes tables, dans un ordre respectant les FK.
DROP TABLE user_role;
DROP TABLE role;
DROP TABLE users;
DROP TABLE books;

-- hibernate_sequence (partagee par l'ancien GenerationType.AUTO de
-- Users/Books) n'est plus utilisee par aucune entite. On la laisse en
-- place (ddl-auto=update ne la supprime jamais, inoffensive).

COMMIT;

-- 7. Verification (lecture seule, apres commit).
SELECT 'administrator' AS table_name, count(*) FROM administrator
UNION ALL SELECT 'adherent', count(*) FROM adherent
UNION ALL SELECT 'livre', count(*) FROM livre
UNION ALL SELECT 'reservation', count(*) FROM reservation
UNION ALL SELECT 'borrow', count(*) FROM borrow;

SELECT r.id, r.book_id, l.matricule AS livre_matricule, r.user_id, a.matricule AS adherent_matricule, r.statut
FROM reservation r
JOIN livre l ON l.book_id = r.book_id
JOIN adherent a ON a.user_id = r.user_id
ORDER BY r.id;
