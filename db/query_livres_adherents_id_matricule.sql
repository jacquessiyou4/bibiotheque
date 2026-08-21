-- Liste des livres L1 a L5 : id, matricule, nom
SELECT book_id AS id, matricule, book_name AS nom
FROM books
WHERE matricule IN ('L1','L2','L3','L4','L5')
ORDER BY matricule, book_id;

-- Liste des adherents A1 a A3 : id, matricule, nom
SELECT user_id AS id, matricule, name AS nom
FROM users
WHERE matricule IN ('A1','A2','A3')
ORDER BY matricule, user_id;
