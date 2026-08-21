-- Seed initial de la base « bibliotheque » (PostgreSQL).
-- Exécuté APRÈS que le backend (Hibernate ddl-auto=update) a créé les tables.
-- Idempotent : peut être rejoué sans casser (ON CONFLICT DO NOTHING).

-- 1. Rôles
INSERT INTO role (role_id, role_name) VALUES
    (1, 'Admin'),
    (2, 'User')
ON CONFLICT (role_id) DO NOTHING;

-- Recale la séquence d'identité de role au-delà des ids posés à la main
DO $$
DECLARE seqname text;
BEGIN
    seqname := pg_get_serial_sequence('role', 'role_id');
    IF seqname IS NOT NULL THEN
        PERFORM setval(seqname, GREATEST((SELECT COALESCE(MAX(role_id), 1) FROM role), 1));
    END IF;
END $$;

-- 2. Compte administrateur (mot de passe BCrypt de « admin123 »)
INSERT INTO users (user_id, username, name, password) VALUES
    (1, 'admin', 'Administrateur', '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON CONFLICT (user_id) DO NOTHING;

-- 3. Liaison admin -> rôle Admin
INSERT INTO user_role (user_id, role_id)
SELECT 1, r.role_id FROM role r WHERE r.role_name = 'Admin'
ON CONFLICT DO NOTHING;

-- 4. Quelques livres pour une application immédiatement utilisable
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (1, 'Clean Code',        'Robert C. Martin',          'Informatique',     5),
    (2, 'Le Petit Prince',   'Antoine de Saint-Exupéry',  'Conte',            3),
    (3, '1984',              'George Orwell',             'Science-fiction',  4),
    (4, 'L''Étranger',       'Albert Camus',              'Roman',            2)
ON CONFLICT (book_id) DO NOTHING;

-- 5. Avance la séquence partagée d'Hibernate (users + books, GenerationType.AUTO)
--    au-delà des identifiants posés manuellement, pour éviter les collisions.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relkind = 'S' AND relname = 'hibernate_sequence') THEN
        PERFORM setval('hibernate_sequence', 100);
    END IF;
END $$;
