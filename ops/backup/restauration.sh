#!/bin/sh
# Restaure un dump produit par sauvegarde.sh dans une base PostgreSQL.
#
#   restauration.sh <fichier.dump> <hôte> <base> <utilisateur>
#   (mot de passe lu dans PGPASSWORD)
#
# La base cible doit exister et être VIDE : on restaure d'abord dans une base
# de vérification, jamais directement par-dessus la production (docs/RUNBOOK.md).
set -eu

if [ "$#" -ne 4 ]; then
    echo "Usage : restauration.sh <fichier.dump> <hôte> <base> <utilisateur>" >&2
    exit 2
fi

fichier="$1"; hote="$2"; base="$3"; utilisateur="$4"

pg_restore --list "$fichier" > /dev/null
pg_restore -h "$hote" -U "$utilisateur" -d "$base" --no-owner --exit-on-error "$fichier"
echo "[restauration] $fichier restauré dans $base@$hote"
