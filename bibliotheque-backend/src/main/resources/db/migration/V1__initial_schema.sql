-- V1 : Schéma initial de la bibliothèque
-- Généré à partir des entités JPA existantes

CREATE TABLE role (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(255)
);

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255)
);

CREATE TABLE user_role (
    user_id INTEGER NOT NULL REFERENCES users(user_id),
    role_id INTEGER NOT NULL REFERENCES role(role_id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE books (
    book_id SERIAL PRIMARY KEY,
    book_name VARCHAR(255) NOT NULL,
    book_author VARCHAR(255) NOT NULL,
    book_genre VARCHAR(255),
    no_of_copies INTEGER
);

CREATE TABLE borrow (
    borrow_id SERIAL PRIMARY KEY,
    book_id INTEGER,
    user_id INTEGER,
    issue_date TIMESTAMP,
    return_date TIMESTAMP,
    due_date TIMESTAMP
);

CREATE TABLE reservation (
    id SERIAL PRIMARY KEY,
    livre_id INTEGER NOT NULL REFERENCES books(book_id),
    adherent_id INTEGER NOT NULL REFERENCES users(user_id),
    date_reservation TIMESTAMP NOT NULL,
    date_expiration TIMESTAMP NOT NULL,
    statut VARCHAR(255) NOT NULL
);
