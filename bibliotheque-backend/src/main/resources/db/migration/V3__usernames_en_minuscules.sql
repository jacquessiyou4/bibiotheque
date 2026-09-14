-- V3 : usernames locaux en minuscules
-- Keycloak enregistre les usernames en MINUSCULES : le jeton du compte « A1 »
-- porte preferred_username = "a1". Le backend retrouve l'utilisateur local par
-- ce username (/me, emprunts, réservations) ; « A1 » (V2) ne correspondait
-- donc jamais et les adhérents ne pouvaient pas utiliser l'application.
UPDATE users SET username = LOWER(username) WHERE username <> LOWER(username);
