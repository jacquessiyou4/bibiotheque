-- V2 : données de départ
-- L'authentification est déléguée à Keycloak, mais le backend a besoin d'un
-- utilisateur LOCAL portant le même username pour chaque compte du realm
-- (voir /me, emprunts, réservations). On reprend donc les comptes de
-- keycloak/realm-bibliotheque.json : admin, A1, A2.

INSERT INTO role (role_name) VALUES ('Admin'), ('User'), ('ADHERENT'), ('BIBLIOTHECAIRE');

-- Mot de passe local (BCrypt) : sert uniquement à l'ancien endpoint
-- /authenticate ; hachage de « admin123 ».
INSERT INTO users (username, name, password) VALUES
    ('admin', 'Admin Bibliothèque', '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    ('A1', 'Adhérent Un', NULL),
    ('A2', 'Adhérent Deux', NULL);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON (u.username = 'admin' AND r.role_name IN ('Admin', 'BIBLIOTHECAIRE'))
            OR (u.username IN ('A1', 'A2') AND r.role_name IN ('User', 'ADHERENT'));

-- Catalogue de démonstration. Les livres à 0 exemplaire permettent de tester
-- les réservations (RG-01 : on ne réserve qu'un livre indisponible).
INSERT INTO books (book_name, book_author, book_genre, no_of_copies) VALUES
    ('Le Petit Prince', 'Antoine de Saint-Exupéry', 'Conte', 3),
    ('Les Misérables', 'Victor Hugo', 'Roman', 2),
    ('L''Étranger', 'Albert Camus', 'Roman', 1),
    ('Une si longue lettre', 'Mariama Bâ', 'Roman', 0),
    ('L''Enfant noir', 'Camara Laye', 'Autobiographie', 0);
