#!/usr/bin/env python3
"""Copia a resources las imagenes de vehiculos y circuitos, redimensionadas.

Los originales viven en docs/assets con nombres inconsistentes (principal vs
imagen-principal, .jpg vs .jpeg, una mayuscula suelta en Spa). Aqui se
normalizan a un esquema estable para que el codigo Java resuelva la ruta sin
escanear directorios en tiempo de ejecucion.
"""
import re
import subprocess
import unicodedata
from pathlib import Path

RAIZ = Path("/home/Daniel/Documentos/java/Formula1Simulator")
ORIGEN_V = RAIZ / "docs/assets/F1_Recursos_Multimedia/img-vehiculos"
ORIGEN_C = RAIZ / "docs/assets/F1_Recursos_Multimedia/img-circuitos"
DESTINO = RAIZ / "simulator/src/main/resources/images"

ANCHO_MAX_VEHICULO = 1600
ANCHO_MAX_CIRCUITO = 1200


def slug(texto):
    """Quita acentos, pasa a minusculas y une con guiones."""
    sin_acentos = "".join(
        c for c in unicodedata.normalize("NFD", texto)
        if unicodedata.category(c) != "Mn")
    sin_prefijo = re.sub(r"^circuito de ", "", sin_acentos.strip().lower())
    return re.sub(r"[^a-z0-9]+", "-", sin_prefijo).strip("-")


def redimensionar(origen, destino, ancho_max):
    destino.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["magick", str(origen), "-resize", f"{ancho_max}x{ancho_max}>",
         "-quality", "88", "-strip", str(destino)],
        check=True)
    return destino.stat().st_size


def vehiculos():
    total = 0
    for carpeta in sorted(ORIGEN_V.iterdir()):
        if not carpeta.is_dir():
            continue
        archivos = sorted(f for f in carpeta.iterdir() if f.is_file())
        principal = [f for f in archivos if "principal" in f.name.lower()]
        auxiliares = [f for f in archivos if "auxiliar" in f.name.lower()]
        # El sufijo numerico decide el orden; sin numero cuenta como el primero.
        auxiliares.sort(key=lambda f: int(
            (re.search(r"(\d+)(?=\.[^.]+$)", f.name) or ["", "1"])[1]))

        if not principal:
            print(f"  !! {carpeta.name}: sin imagen principal")
            continue
        salida = DESTINO / "vehicles" / carpeta.name
        total += redimensionar(principal[0],
                               salida / f"principal{principal[0].suffix.lower()}",
                               ANCHO_MAX_VEHICULO)
        for i, aux in enumerate(auxiliares, start=1):
            total += redimensionar(aux, salida / f"auxiliar-{i}{aux.suffix.lower()}",
                                   ANCHO_MAX_VEHICULO)
        print(f"  {carpeta.name}: 1 principal + {len(auxiliares)} auxiliares")
    return total


def circuitos():
    total = 0
    for archivo in sorted(ORIGEN_C.iterdir()):
        if not archivo.is_file():
            continue
        nombre = slug(re.sub(r"^circuito-de-", "", archivo.stem))
        salida = DESTINO / "circuits" / f"{nombre}{archivo.suffix.lower()}"
        total += redimensionar(archivo, salida, ANCHO_MAX_CIRCUITO)
        print(f"  {archivo.name} -> circuits/{salida.name}")
    return total


if __name__ == "__main__":
    print("Vehiculos:")
    v = vehiculos()
    print("Circuitos:")
    c = circuitos()
    print(f"\nTotal copiado: {(v + c) / 1048576:.1f} MB")
