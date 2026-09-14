#!/bin/sh
# Exécuté automatiquement par l'image nginx officielle au démarrage du
# conteneur (tout script exécutable dans /docker-entrypoint.d/ est lancé
# avant nginx). Objectif : injecter l'URL du backend SANS rebuild de l'image
# Angular, puisque c'est le NAVIGATEUR (pas le conteneur) qui exécutera ce
# code et qui doit savoir où joindre le backend. Le navigateur ne contacte
# plus Keycloak : la connexion passe par le backend (POST /auth/token).
set -e

API_URL="${API_URL:-http://localhost:8080}"

# API_URL est recopiée dans un fichier JS et dans un en-tête HTTP : refuser
# tout caractère qui permettrait d'en sortir (guillemets, point-virgule...).
case "$API_URL" in
  *[!A-Za-z0-9:/._-]*)
    echo "[env-config] API_URL invalide : ${API_URL}" >&2
    exit 1
    ;;
esac

cat > /usr/share/nginx/html/assets/env.js <<EOF
window.__env = {
  apiUrl: "${API_URL}"
};
EOF

# connect-src n'accepte qu'une origine (schéma://hôte:port), sans chemin.
API_ORIGIN=$(printf '%s' "$API_URL" | sed -E 's#^([a-z]+://[^/]+).*#\1#')

mkdir -p /etc/nginx/security-headers
cat > /etc/nginx/security-headers/csp.conf <<EOF
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data:; connect-src 'self' ${API_ORIGIN}; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'" always;
EOF

echo "[env-config] assets/env.js généré avec apiUrl=${API_URL} ; CSP connect-src 'self' ${API_ORIGIN}"
