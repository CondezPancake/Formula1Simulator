#!/usr/bin/env bash
# Descarga los logos oficiales de las escuderías desde formula1.com.
#
# Los que había eran de 96x96 y ~1 KB: en una tarjeta se veían borrosos.
# Las imágenes de F1 se sirven por Cloudinary, así que la transformación va
# en la propia URL y se pueden pedir a la resolución que interese.
#
# Se piden en PNG a propósito: el sitio entrega WebP por defecto y JavaFX no
# lo decodifica. Y se descarga a un temporal que solo sustituye al fichero
# bueno si la descarga cuela, para no dejar un logo a medias si falla la red.
#
# Uso:  tools/descargar_logos.sh
set -u

DESTINO="simulator/src/main/resources/images/teams"
BASE="https://media.formula1.com/image/upload/w_512/f_png/q_auto/v1740000001/common/f1/2026"

# slug del sitio de F1 -> nombre del fichero que ya usa F1Assets.
#
# Los dos últimos no son un error: en el seed la parrilla es la de 2024 y
# esas escuderías cambiaron de nombre. Alfa Romeo pasó a ser Audi y
# AlphaTauri, Racing Bulls; son los enlaces que corresponden hoy.
MAPA="
redbullracing:redbullracing
mercedes:mercedes
ferrari:ferrari
mclaren:mclaren
astonmartin:astonmartin
alpine:alpine
haasf1team:haas
williams:williams
audi:kicksauber
racingbulls:rb
"

ok=0
fallidos=0

for par in $MAPA; do
    slug="${par%%:*}"
    fichero="${par##*:}"
    url="$BASE/$slug/2026${slug}logowhite.webp"
    tmp="$(mktemp)"

    if curl -sfL --max-time 30 -o "$tmp" "$url" && [ -s "$tmp" ]; then
        mv "$tmp" "$DESTINO/$fichero.png"
        printf '  ok       %-16s %s bytes\n' "$fichero.png" "$(wc -c < "$DESTINO/$fichero.png")"
        ok=$((ok + 1))
    else
        rm -f "$tmp"
        printf '  FALLÓ    %-16s se conserva el que ya había\n' "$fichero.png"
        fallidos=$((fallidos + 1))
    fi
done

echo
echo "$ok descargados, $fallidos fallidos."
[ "$fallidos" -gt 0 ] && echo "Los fallidos conservan su versión anterior: la app no depende de tener red."
exit 0
