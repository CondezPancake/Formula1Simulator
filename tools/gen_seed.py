#!/usr/bin/env python3
"""Genera seed.json a partir de los datos literales de f1project.md,
completando los equipos y vehiculos que la spec deja sin definir."""
import json, collections

# ---------------------------------------------------------------- pilotos
# (id, nombre, equipo, rol, experiencia, velocidad, consistencia, lluvia)
PILOTOS = [
    (1,  "Max Verstappen",   "Red Bull Racing",        "Líder",    9, 98, 95, 96),
    (2,  "Sergio Pérez",     "Red Bull Racing",        "Escudero",10, 88, 80, 82),
    (3,  "Lewis Hamilton",   "Mercedes-AMG Petronas",  "Líder",   17, 95, 93, 97),
    (4,  "George Russell",   "Mercedes-AMG Petronas",  "Escudero", 5, 90, 88, 86),
    (5,  "Charles Leclerc",  "Ferrari",                "Líder",    6, 94, 85, 88),
    (6,  "Carlos Sainz",     "Ferrari",                "Escudero", 9, 89, 90, 85),
    (7,  "Lando Norris",     "McLaren",                "Líder",    5, 92, 89, 90),
    (8,  "Oscar Piastri",    "McLaren",                "Escudero", 1, 87, 84, 80),
    (9,  "Fernando Alonso",  "Aston Martin",           "Líder",   20, 93, 94, 95),
    (10, "Lance Stroll",     "Aston Martin",           "Escudero", 7, 80, 75, 78),
    (11, "Esteban Ocon",     "Alpine",                 "Líder",    7, 84, 82, 83),
    (12, "Pierre Gasly",     "Alpine",                 "Escudero", 7, 85, 81, 84),
    (13, "Valtteri Bottas",  "Alfa Romeo",             "Líder",   11, 86, 87, 84),
    (14, "Zhou Guanyu",      "Alfa Romeo",             "Escudero", 2, 78, 76, 74),
    (15, "Kevin Magnussen",  "Haas",                   "Líder",    8, 82, 74, 79),
    (16, "Nico Hülkenberg",  "Haas",                   "Escudero",12, 84, 83, 86),
    (17, "Yuki Tsunoda",     "AlphaTauri",             "Líder",    4, 83, 75, 77),
    (18, "Daniel Ricciardo", "AlphaTauri",             "Escudero",13, 86, 82, 85),
    (19, "Alexander Albon",  "Williams",               "Líder",    5, 85, 84, 82),
    (20, "Logan Sargeant",   "Williams",               "Escudero", 1, 74, 70, 68),
]

# ---------------------------------------------------------------- equipos
# (nombre, pais, motor)  -- los 3 primeros son literales de la spec
EQUIPOS = [
    ("Red Bull Racing",       "Austria",      "Honda"),
    ("Mercedes-AMG Petronas", "Alemania",     "Mercedes"),
    ("Ferrari",               "Italia",       "Ferrari"),
    ("McLaren",               "Reino Unido",  "Mercedes"),
    ("Aston Martin",          "Reino Unido",  "Mercedes"),
    ("Alpine",                "Francia",      "Renault"),
    ("Alfa Romeo",            "Suiza",        "Ferrari"),
    ("Haas",                  "Estados Unidos","Ferrari"),
    ("AlphaTauri",            "Italia",       "Honda"),
    ("Williams",              "Reino Unido",  "Mercedes"),
]

IMG_EQUIPO = {
    "Red Bull Racing": "https://upload.wikimedia.org/wikipedia/commons/b/bb/Red_Bull_Racing_Logo.svg",
    "Mercedes-AMG Petronas": "https://upload.wikimedia.org/wikipedia/commons/3/32/Mercedes_AMG_Petronas_F1_Team_logo.svg",
    "Ferrari": "https://upload.wikimedia.org/wikipedia/en/d/d4/Scuderia_Ferrari_Logo.svg",
}

# ---------------------------------------------------------------- vehiculos
# (modelo, equipo, vel_max, acel, vel_agresiva, escala_consumo/desgaste)
# RB20 y W15 reproducen exactamente los valores de la spec (escala 1.00 y 1.05).
VEHICULOS = [
    ("RB20",  "Red Bull Racing",       360, 2.5, 340, 1.00),
    ("W15",   "Mercedes-AMG Petronas", 355, 2.6, 335, 1.05),
    ("SF-24", "Ferrari",               357, 2.5, 337, 1.02),
    ("MCL38", "McLaren",               356, 2.6, 336, 1.03),
    ("AMR24", "Aston Martin",          352, 2.7, 332, 1.07),
    ("AT04",  "AlphaTauri",            349, 2.8, 329, 1.08),
    ("A524",  "Alpine",                350, 2.8, 330, 1.09),
    ("C43",   "Alfa Romeo",            348, 2.8, 328, 1.10),
    ("VF-24", "Haas",                  347, 2.9, 327, 1.11),
    ("FW46",  "Williams",              346, 2.9, 326, 1.12),
]

# Valores base del RB20 en la spec: [seco, lluvioso, extremo] por modo
BASE = {
    "conduccion_agresiva": {"consumo": [2.4, 2.6, 3.0], "desgaste": [2.2, 1.2, 3.5]},
    "conduccion_normal":   {"consumo": [1.9, 2.1, 2.4], "desgaste": [1.5, 0.8, 2.5]},
    "ahorro_combustible":  {"consumo": [1.6, 1.8, 2.1], "desgaste": [1.0, 0.5, 1.8]},
}
CLIMAS = ["seco", "lluvioso", "extremo"]

# Los dos vehiculos que la spec define se copian LITERALMENTE, sin escalar.
LITERAL = {
    "RB20": BASE,
    "W15": {
        "conduccion_normal":   {"consumo": [2.0, 2.2, 2.5], "desgaste": [1.6, 0.9, 2.6]},
        "conduccion_agresiva": {"consumo": [2.6, 2.8, 3.2], "desgaste": [2.3, 1.4, 3.8]},
        "ahorro_combustible":  {"consumo": [1.7, 1.9, 2.2], "desgaste": [1.1, 0.6, 1.9]},
    },
}

# ---------------------------------------------------------------- circuitos
# (nombre, pais, long_km, vueltas, descripcion, record, piloto, anio,
#  ganadores, prob_clima, f_consumo, f_desgaste, imagen)
CIRCUITOS = [
    ("Circuito de Mónaco", "Mónaco", 3.34, 78,
     "Uno de los circuitos más prestigiosos y difíciles del calendario, conocido por sus calles angostas y la falta de zonas de adelantamiento.",
     "1:10.166", "Lewis Hamilton", 2019, [(2021,1),(2022,2),(2023,1)],
     [0.75,0.20,0.05], 0.90, 0.85,
     "https://upload.wikimedia.org/wikipedia/commons/4/4e/Monte_Carlo_Formula_1_track_map.svg"),
    ("Silverstone", "Reino Unido", 5.89, 52,
     "Uno de los circuitos más rápidos del calendario, con curvas de alta velocidad como Maggotts y Becketts.",
     "1:27.097", "Max Verstappen", 2020, [(2021,3),(2022,5),(2023,1)],
     [0.55,0.35,0.10], 1.10, 1.20,
     "https://upload.wikimedia.org/wikipedia/commons/5/5e/Silverstone_Circuit_2020_layout.png"),
    ("Circuito de Spa-Francorchamps", "Bélgica", 7.00, 44,
     "Famoso por la curva Eau Rouge y la larga recta de Kemmel, un circuito donde la potencia del motor es clave.",
     "1:46.286", "Valtteri Bottas", 2018, [(2021,1),(2022,1),(2023,1)],
     [0.50,0.35,0.15], 1.12, 1.10,
     "https://upload.wikimedia.org/wikipedia/commons/1/1e/Circuit_Spa_2018.png"),
    ("Circuito de Monza", "Italia", 5.79, 53,
     "Conocido como 'El Templo de la Velocidad', Monza es el circuito más rápido del calendario con largas rectas y chicanes icónicas.",
     "1:21.046", "Rubens Barrichello", 2004, [(2021,2),(2022,1),(2023,1)],
     [0.80,0.15,0.05], 1.15, 1.00,
     "https://upload.wikimedia.org/wikipedia/commons/3/3e/Monza_track_map.svg"),
    ("Interlagos", "Brasil", 4.31, 71,
     "Interlagos es un circuito legendario con cambios de elevación y un trazado técnico que ha sido sede de algunas de las carreras más emocionantes de la historia.",
     "1:10.540", "Valtteri Bottas", 2018, [(2021,3),(2022,1),(2023,1)],
     [0.60,0.30,0.10], 1.02, 1.05,
     "https://upload.wikimedia.org/wikipedia/commons/2/23/Aut%C3%B3dromo_Jos%C3%A9_Carlos_Pace_%28Interlagos%29.svg"),
    ("Circuito de Yas Marina", "Emiratos Árabes Unidos", 5.28, 58,
     "Ubicado en Abu Dhabi, es famoso por ser el circuito donde se definen muchos campeonatos, con un diseño moderno y una espectacular carrera nocturna.",
     "1:39.283", "Lewis Hamilton", 2019, [(2021,1),(2022,1),(2023,3)],
     [0.95,0.04,0.01], 0.98, 0.95,
     "https://upload.wikimedia.org/wikipedia/commons/0/0a/Yas_Marina_Circuit_2021_layout.svg"),
    ("Circuito de Suzuka", "Japón", 5.81, 53,
     "Un circuito desafiante con un diseño en forma de ocho, famoso por sus curvas de alta velocidad como 130R y la 'S' de Senna.",
     "1:30.983", "Lewis Hamilton", 2019, [(2021,1),(2022,1),(2023,1)],
     [0.65,0.25,0.10], 1.05, 1.15,
     "https://upload.wikimedia.org/wikipedia/commons/e/eb/Suzuka_circuit_map--2005.svg"),
]

VEL_REF = 340.0


def parse_tiempo(t):
    m, s = t.split(":")
    return int(m) * 60 + float(s)


def build():
    pilotos = [collections.OrderedDict([
        ("id", i), ("nombre", n), ("equipo", e), ("rol", r), ("experiencia", x),
        ("habilidades", collections.OrderedDict(
            [("velocidad", v), ("consistencia", c), ("lluvia", l)])),
    ]) for (i, n, e, r, x, v, c, l) in PILOTOS]

    por_equipo = collections.defaultdict(list)
    for p in PILOTOS:
        por_equipo[p[2]].append(p[0])

    equipos = [collections.OrderedDict([
        ("nombre", n), ("pais", pa), ("motor", mo),
        ("pilotos", sorted(por_equipo[n])),
        ("imagen", IMG_EQUIPO.get(n, "")),
    ]) for (n, pa, mo) in EQUIPOS]

    vehiculos = []
    for (modelo, equipo, vmax, acel, v_agr, escala) in VEHICULOS:
        velocidades = {
            "conduccion_agresiva": v_agr,
            "conduccion_normal": v_agr - 20,
            "ahorro_combustible": v_agr - 40,
        }
        literal = LITERAL.get(modelo)
        rendimiento = collections.OrderedDict()
        for modo in ("conduccion_normal", "conduccion_agresiva", "ahorro_combustible"):
            if literal:
                consumo = literal[modo]["consumo"]
                desgaste = literal[modo]["desgaste"]
            else:
                consumo = [round(BASE[modo]["consumo"][k] * escala, 1) for k in range(3)]
                desgaste = [round(BASE[modo]["desgaste"][k] * escala, 1) for k in range(3)]
            rendimiento[modo] = collections.OrderedDict([
                ("velocidad_promedio_kmh", velocidades[modo]),
                ("consumo_combustible", collections.OrderedDict(zip(CLIMAS, consumo))),
                ("desgaste_neumaticos", collections.OrderedDict(zip(CLIMAS, desgaste))),
            ])
        vehiculos.append(collections.OrderedDict([
            ("modelo", modelo), ("equipo", equipo),
            ("motor", dict((n, m) for (n, _, m) in EQUIPOS)[equipo]),
            ("velocidad_maxima_kmh", vmax), ("aceleracion_0_100", acel),
            ("pilotos", sorted(por_equipo[equipo])),
            ("rendimiento", rendimiento), ("imagen", ""),
        ]))

    circuitos = []
    for (nom, pais, lkm, vlt, desc, rec, rpil, ranio, gan, prob, fc, fd, img) in CIRCUITOS:
        seg = parse_tiempo(rec)
        factor = round(seg / (3600.0 * lkm / VEL_REF), 3)
        circuitos.append(collections.OrderedDict([
            ("nombre", nom), ("pais", pais), ("longitud_km", lkm), ("vueltas", vlt),
            ("descripcion", desc),
            ("record_vuelta", collections.OrderedDict(
                [("tiempo", rec), ("piloto", rpil), ("anio", ranio)])),
            ("ganadores", [collections.OrderedDict([("temporada", t), ("piloto", p)])
                           for (t, p) in gan]),
            ("probabilidad_clima", collections.OrderedDict(
                zip(CLIMAS, prob))),
            ("factor_tecnico", factor),
            ("factor_consumo", fc), ("factor_desgaste", fd),
            ("imagen", img),
        ]))

    return collections.OrderedDict([
        ("pilotos", pilotos), ("equipos", equipos),
        ("vehiculos", vehiculos), ("circuitos", circuitos),
    ])


if __name__ == "__main__":
    import sys
    data = build()
    with open(sys.argv[1], "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("pilotos  :", len(data["pilotos"]))
    print("equipos  :", len(data["equipos"]))
    print("vehiculos:", len(data["vehiculos"]))
    print("circuitos:", len(data["circuitos"]))
    for c in data["circuitos"]:
        print(f'  {c["nombre"][:32]:34s} factor_tecnico={c["factor_tecnico"]}')
