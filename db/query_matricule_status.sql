-- Matricule + statut des livres (L1-L5) et adherents (A1-A3).
SELECT DISTINCT 'Livre' AS type, matricule,
  CASE WHEN no_of_copies >= 1 THEN 'Disponible' ELSE 'Non-disponible' END AS status
FROM books
WHERE matricule IN ('L1','L2','L3','L4','L5')
UNION ALL
SELECT DISTINCT 'Adherent' AS type, matricule,
  CASE matricule
    WHEN 'A1' THEN 'Réservataire principal'
    WHEN 'A2' THEN 'Saturation du quota (3 réservations)'
    WHEN 'A3' THEN 'Emprunteur des livres L2-L5'
  END AS status
FROM users
WHERE matricule IN ('A1','A2','A3')
ORDER BY type, matricule;
