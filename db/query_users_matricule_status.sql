-- Matricule + statut des adherents uniquement (A1-A3).
SELECT DISTINCT matricule,
  CASE matricule
    WHEN 'A1' THEN 'Réservataire principal'
    WHEN 'A2' THEN 'Saturation du quota (3 réservations)'
    WHEN 'A3' THEN 'Emprunteur des livres L2-L5'
  END AS status
FROM users
WHERE matricule IN ('A1','A2','A3')
ORDER BY matricule;
