#!/usr/bin/env python3
"""Sincroniza en los DML SQL los campos de ficha que proceden del seed."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SEED = ROOT / "simulator/src/main/resources/data/seed.json"
TARGETS = (
    ROOT / "database/SQL/data.sql",
    ROOT / "database/mysql_conectar facil/02_datos_iniciales.sql",
)
START = "-- Fichas ampliadas generadas desde seed.json\n"
END = "-- Fin de fichas ampliadas\n"
ANCHOR = "-- Las vistas de control deben devolver cero filas tras esta carga.\n"


def quote(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def assignment(column, value):
    return f"{column} = {quote(value)}"


def build_block(seed):
    lines = [START.rstrip()]
    for team in seed["equipos"]:
        values = [
            assignment("nombre_completo", team.get("nombre_completo")),
            assignment("base", team.get("base")),
            assignment("jefe_equipo", team.get("jefe_equipo")),
            assignment("jefe_tecnico", team.get("jefe_tecnico")),
            assignment("piloto_reserva", team.get("piloto_reserva")),
            f"primera_participacion = {team.get('primera_participacion', 0)}",
            f"campeonatos = {team.get('campeonatos', 0)}",
            f"gran_premios = {team.get('gran_premios', 0)}",
            f"victorias = {team.get('victorias', 0)}",
            f"podios = {team.get('podios', 0)}",
            f"poles = {team.get('poles', 0)}",
            assignment("descripcion", team.get("descripcion")),
        ]
        lines.append(f"UPDATE equipo SET {', '.join(values)} WHERE nombre = {quote(team['nombre'])};")
    for driver in seed["pilotos"]:
        values = [
            assignment("fecha_nacimiento", driver.get("fechaNacimiento")),
            assignment("lugar_nacimiento", driver.get("lugarNacimiento")),
            assignment("biografia", driver.get("biografia")),
        ]
        lines.append(f"UPDATE piloto SET {', '.join(values)} WHERE piloto_id = {driver['id']};")
    lines.append(END.rstrip())
    return "\n".join(lines) + "\n\n"


def update(path, block):
    text = path.read_text(encoding="utf-8")
    if START in text:
        before, rest = text.split(START, 1)
        _, after = rest.split(END, 1)
        text = before + after.lstrip("\n")
    if ANCHOR not in text:
        raise RuntimeError(f"No se encontró el punto de inserción en {path}")
    path.write_text(text.replace(ANCHOR, block + ANCHOR), encoding="utf-8")


seed_data = json.loads(SEED.read_text(encoding="utf-8"))
generated = build_block(seed_data)
for target in TARGETS:
    update(target, generated)
