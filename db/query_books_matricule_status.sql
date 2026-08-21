-- Matricule + statut des livres uniquement (L1-L5).
SELECT DISTINCT matricule,
  CASE WHEN no_of_copies >= 1 THEN 'Disponible' ELSE 'Non-disponible' END AS status
FROM books
WHERE matricule IN ('L1','L2','L3','L4','L5')
ORDER BY matricule;
