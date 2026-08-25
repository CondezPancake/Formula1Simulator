#!/usr/bin/env python3
"""Descarga los assets oficiales de formula1.com que usa la tarjeta de piloto.

JavaFX no lee WebP, asi que a Cloudinary se le pide PNG con la transformacion
`f_png` y la textura DRS se convierte con ImageMagick. Los ficheros se dejan en
el classpath de la app (`simulator/src/main/resources/images/`), de modo que la
aplicacion arranca sin red una vez ejecutado este script.

Uso:  python3 tools/descargar_assets_f1.py [--forzar]
"""

import argparse
import shutil
import subprocess
import sys
import urllib.parse
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
RECURSOS = RAIZ / "simulator" / "src" / "main" / "resources" / "images"

CDN = "https://media.formula1.com/image/upload"
DAM = ("https://media.formula1.com/content/dam/fom-website"
       "/2018-redesign-assets/Flags%2016x9")
DRS = ("https://www.formula1.com/assets/driverTeam/_next/static/media"
       "/DRS-G-2x~444bb8a2f38894fc.444bb8a2.webp")

# El seed usa nombres de equipo de 2023, pero el CDN solo tiene carpeta 2024.
SLUG_EQUIPO = {
    "Red Bull Racing": "redbullracing",
    "Mercedes-AMG Petronas": "mercedes",
    "Ferrari": "ferrari",
    "McLaren": "mclaren",
    "Aston Martin": "astonmartin",
    "Alpine": "alpine",
    "Alfa Romeo": "kicksauber",
    "Haas": "haas",
    "AlphaTauri": "rb",
    "Williams": "williams",
}

# Codigo del piloto -> (equipo del seed, slug del CDN). Los tres ultimos no
# siguen el patron obvio: Alonso es feralo01, Zhou guazho01, Russell georus01.
PILOTOS = {
    "VER": ("Red Bull Racing", "maxver01"),
    "PER": ("Red Bull Racing", "serper01"),
    "HAM": ("Mercedes-AMG Petronas", "lewham01"),
    "RUS": ("Mercedes-AMG Petronas", "georus01"),
    "LEC": ("Ferrari", "chalec01"),
    "SAI": ("Ferrari", "carsai01"),
    "NOR": ("McLaren", "lannor01"),
    "PIA": ("McLaren", "oscpia01"),
    "ALO": ("Aston Martin", "feralo01"),
    "STR": ("Aston Martin", "lanstr01"),
    "OCO": ("Alpine", "estoco01"),
    "GAS": ("Alpine", "piegas01"),
    "BOT": ("Alfa Romeo", "valbot01"),
    "ZHO": ("Alfa Romeo", "guazho01"),
    "MAG": ("Haas", "kevmag01"),
    "HUL": ("Haas", "nichul01"),
    "TSU": ("AlphaTauri", "yuktsu01"),
    "RIC": ("AlphaTauri", "danric01"),
    "ALB": ("Williams", "alealb01"),
    "SAR": ("Williams", "logsar01"),
}

# Nacionalidad tal cual aparece en seed.json -> nombre de fichero de bandera.
PAIS = {
    "Neerlandes": "netherlands",
    "Mexicano": "mexico",
    "Britanico": "great-britain",
    "Monegasco": "monaco",
    "Espanol": "spain",
    "Australiano": "australia",
    "Canadiense": "canada",
    "Frances": "france",
    "Finlandes": "finland",
    "Chino": "china",
    "Danes": "denmark",
    "Aleman": "germany",
    "Japones": "japan",
    "Tailandes": "thailand",
    "Estadounidense": "united-states-of-america",
}

AGENTE = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
          "(KHTML, like Gecko) Chrome/120.0 Safari/537.36")


def bajar(url, destino, forzar):
    """Descarga si falta. Devuelve True si el fichero acaba en su sitio."""
    if destino.exists() and not forzar:
        return True
    destino.parent.mkdir(parents=True, exist_ok=True)
    peticion = urllib.request.Request(url, headers={"User-Agent": AGENTE})
    try:
        with urllib.request.urlopen(peticion, timeout=30) as respuesta:
            datos = respuesta.read()
    except Exception as error:                        # noqa: BLE001
        print(f"  ! {destino.name}: {error}", file=sys.stderr)
        return False
    # Un PNG de menos de 1 KB del CDN suele ser el marcador transparente.
    if len(datos) < 1024:
        print(f"  ! {destino.name}: respuesta de {len(datos)} B, se descarta",
              file=sys.stderr)
        return False
    destino.write_bytes(datos)
    return True


# Ancho comun de los renders ya normalizados. La tarjeta los pinta a la mitad;
# el doble deja margen para pantallas de alta densidad.
ANCHO_RENDER = 320
# Alto util: por debajo de la rodilla la tarjeta ya lo ha recortado, asi que
# guardar mas solo engorda el repositorio (6 MB frente a 1,6 MB).
ALTO_RENDER = 560


def normalizar(fichero):
    """Iguala el encuadre de los renders.

    El CDN los sirve todos a 620 px de alto, pero unos son de cuerpo entero y
    otros de medio cuerpo, asi que escalarlos por altura deja a unos pilotos
    diminutos y a otros gigantes. Recortando el margen transparente y fijando
    el ancho, los hombros miden lo mismo en los veinte y la tarjeta puede
    anclarlos por la cabeza y dejar que el cuerpo se salga por abajo, que es lo
    que hace la web oficial.
    """
    subprocess.run(["magick", str(fichero), "-trim", "+repage",
                    "-resize", f"{ANCHO_RENDER}x",
                    "-crop", f"{ANCHO_RENDER}x{ALTO_RENDER}+0+0", "+repage",
                    str(fichero)], check=True)


def pilotos(forzar):
    print("Renders de piloto (PNG transparente, encuadre normalizado)")
    if shutil.which("magick") is None:
        print("  ! falta ImageMagick (magick) para normalizar el encuadre",
              file=sys.stderr)
        return False
    ok = 0
    for codigo, (equipo, slug) in PILOTOS.items():
        equipo_slug = SLUG_EQUIPO[equipo]
        url = (f"{CDN}/c_scale,h_620/f_png/common/f1/2024/{equipo_slug}"
               f"/{slug}/2024{equipo_slug}{slug}right.png")
        destino = RECURSOS / "drivers" / "f1" / f"{codigo}.png"
        existia = destino.exists()
        if bajar(url, destino, forzar):
            if not existia or forzar:
                normalizar(destino)
            ok += 1
    print(f"  {ok}/{len(PILOTOS)}")
    return ok == len(PILOTOS)


def banderas(forzar):
    print("Banderas de nacionalidad")
    ok = 0
    for pais in sorted(set(PAIS.values())):
        url = f"{DAM}/{urllib.parse.quote(pais)}-flag.jpg"
        if bajar(url, RECURSOS / "flags" / f"{pais}.jpg", forzar):
            ok += 1
    total = len(set(PAIS.values()))
    print(f"  {ok}/{total}")
    return ok == total


def logos(forzar):
    print("Logos de equipo (version blanca)")
    ok = 0
    for slug in sorted(set(SLUG_EQUIPO.values())):
        url = f"{CDN}/f_png/common/f1/2024/{slug}/2024{slug}logowhite.png"
        # Los logos pesan entre 370 B y 1 KB: el umbral general no aplica.
        destino = RECURSOS / "teams" / f"{slug}.png"
        if destino.exists() and not forzar:
            ok += 1
            continue
        destino.parent.mkdir(parents=True, exist_ok=True)
        try:
            peticion = urllib.request.Request(url, headers={"User-Agent": AGENTE})
            with urllib.request.urlopen(peticion, timeout=30) as respuesta:
                destino.write_bytes(respuesta.read())
            ok += 1
        except Exception as error:                    # noqa: BLE001
            print(f"  ! {slug}: {error}", file=sys.stderr)
    print(f"  {ok}/{len(set(SLUG_EQUIPO.values()))}")
    return ok == len(set(SLUG_EQUIPO.values()))


def textura(forzar):
    """Halftone de velocidad de F1.com: mascara blanca con alfa, se tine en la app."""
    print("Textura DRS")
    destino = RECURSOS / "patterns" / "drs-mask.png"
    if destino.exists() and not forzar:
        print("  ya estaba")
        return True
    if shutil.which("magick") is None:
        print("  ! falta ImageMagick (magick) para convertir el WebP",
              file=sys.stderr)
        return False
    temporal = destino.parent / "drs-mask.webp"
    if not bajar(DRS, temporal, True):
        return False
    # El original mide 3422x1687; a 480 de ancho sobra para una tarjeta de 340.
    subprocess.run(["magick", str(temporal), "-resize", "480x", str(destino)],
                   check=True)
    temporal.unlink()
    print("  1/1")
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--forzar", action="store_true",
                        help="vuelve a descargar aunque el fichero ya exista")
    args = parser.parse_args()

    completo = all([pilotos(args.forzar), banderas(args.forzar),
                    logos(args.forzar), textura(args.forzar)])
    print("\nDestino:", RECURSOS)
    return 0 if completo else 1


if __name__ == "__main__":
    sys.exit(main())
