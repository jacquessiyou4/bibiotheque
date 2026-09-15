-- V5 : lien stable entre compte local et compte Keycloak, saisies canoniques
--
-- Le compte local était retrouvé par username seulement. Or un username peut
-- changer dans Keycloak, ou être réattribué après suppression d'un compte :
-- le nouveau titulaire aurait hérité des emprunts de l'ancien. On mémorise
-- donc l'identifiant immuable du jeton (claim « sub »), renseigné à la
-- première requête authentifiée (voir CompteKeycloakFilter).

ALTER TABLE users ADD COLUMN keycloak_sub VARCHAR(64);
CREATE UNIQUE INDEX ux_users_keycloak_sub ON users (keycloak_sub);

-- Formes canoniques, comme les appliquent désormais les DTO de l'API.
-- En cas de doublon révélé par le nettoyage, l'index ux_users_username_lower
-- (V4) fait échouer la migration entière : rien n'est modifié.
UPDATE users SET username = lower(btrim(regexp_replace(username, '\s+', ' ', 'g')))
WHERE username <> lower(btrim(regexp_replace(username, '\s+', ' ', 'g')));

UPDATE users SET name = btrim(regexp_replace(name, '\s+', ' ', 'g'))
WHERE name <> btrim(regexp_replace(name, '\s+', ' ', 'g'));

UPDATE books SET
    book_name = btrim(regexp_replace(book_name, '\s+', ' ', 'g')),
    book_author = btrim(regexp_replace(book_author, '\s+', ' ', 'g')),
    book_genre = NULLIF(btrim(regexp_replace(book_genre, '\s+', ' ', 'g')), '');
