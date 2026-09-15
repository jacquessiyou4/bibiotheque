#!/bin/sh
# Sauvegarde des deux bases PostgreSQL (application et Keycloak).
#
#   sauvegarde.sh once    : une sauvegarde immédiate, puis sortie
#   sauvegarde.sh daemon  : une sauvegarde chaque jour à BACKUP_HEURE (UTC)
#
# Format « custom » de pg_dump (-Fc) : compressé, restaurable table par table
# avec pg_restore (voir restauration.sh). Chaque fichier est vérifié par
# pg_restore --list : une sauvegarde illisible échoue tout de suite, pas le
# jour où l'on en a besoin.
set -eu

DOSSIER=/backups

# Secrets Docker (docker-compose.secrets.yml) : le mot de passe est lu dans un fichier.
if [ -n "${APP_DB_PASSWORD_FILE:-}" ]; then APP_DB_PASSWORD="$(cat "$APP_DB_PASSWORD_FILE")"; fi
if [ -n "${KC_DB_PASSWORD_FILE:-}" ]; then KC_DB_PASSWORD="$(cat "$KC_DB_PASSWORD_FILE")"; fi
JOURS="${BACKUP_JOURS_CONSERVES:-7}"
HEURE="${BACKUP_HEURE:-02}"

sauvegarder_base() {
    nom="$1"; hote="$2"; base="$3"; utilisateur="$4"; mot_de_passe="$5"
    horodatage=$(date -u +%Y%m%dT%H%M%SZ)
    fichier="$DOSSIER/$nom-$horodatage.dump"
    PGPASSWORD="$mot_de_passe" pg_dump -h "$hote" -U "$utilisateur" -d "$base" -Fc -f "$fichier.partiel"
    pg_restore --list "$fichier.partiel" > /dev/null
    mv "$fichier.partiel" "$fichier"
    echo "[sauvegarde] $fichier ($(du -h "$fichier" | cut -f1))"
}

tout_sauvegarder() {
    mkdir -p "$DOSSIER"
    sauvegarder_base bibliotheque "$APP_DB_HOST" "$APP_DB_NAME" "$APP_DB_USER" "$APP_DB_PASSWORD"
    sauvegarder_base keycloak "$KC_DB_HOST" "$KC_DB_NAME" "$KC_DB_USER" "$KC_DB_PASSWORD"
    # Rétention : suppression des sauvegardes plus anciennes que JOURS jours.
    find "$DOSSIER" -name '*.dump' -type f -mtime +"$JOURS" -print -delete | sed 's/^/[sauvegarde] supprimé : /'
}

case "${1:-daemon}" in
    once)
        tout_sauvegarder
        ;;
    daemon)
        echo "[sauvegarde] planifiée chaque jour à ${HEURE}h UTC, conservation ${JOURS} jours"
        while true; do
            if [ "$(date -u +%H)" = "$HEURE" ]; then
                tout_sauvegarder || echo "[sauvegarde] ÉCHEC, nouvelle tentative dans une heure" >&2
                sleep 3600
            fi
            sleep 300
        done
        ;;
    *)
        echo "Usage : sauvegarde.sh once|daemon" >&2
        exit 2
        ;;
esac
