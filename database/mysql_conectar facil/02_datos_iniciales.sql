-- Formula1Simulator - MySQL 8.0.19+
-- DML generado desde data/seed.json y desde los catálogos del dominio Java.
-- Idempotente: puede ejecutarse de nuevo para sincronizar valores sin duplicarlos.

USE formula1_simulator;
SET NAMES utf8mb4;
START TRANSACTION;

-- Catálogos base
INSERT INTO pais (pais_id, nombre) VALUES
    (1, 'Austria'),
    (2, 'Alemania'),
    (3, 'Italia'),
    (4, 'Reino Unido'),
    (5, 'Francia'),
    (6, 'Suiza'),
    (7, 'Estados Unidos'),
    (8, 'Mónaco'),
    (9, 'Bélgica'),
    (10, 'Brasil'),
    (11, 'Emiratos Árabes Unidos'),
    (12, 'Japón')
AS nuevo
ON DUPLICATE KEY UPDATE
    nombre = nuevo.nombre;

INSERT INTO nacionalidad (nacionalidad_id, nombre) VALUES
    (1, 'Neerlandes'),
    (2, 'Mexicano'),
    (3, 'Britanico'),
    (4, 'Monegasco'),
    (5, 'Espanol'),
    (6, 'Australiano'),
    (7, 'Canadiense'),
    (8, 'Frances'),
    (9, 'Finlandes'),
    (10, 'Chino'),
    (11, 'Danes'),
    (12, 'Aleman'),
    (13, 'Japones'),
    (14, 'Tailandes'),
    (15, 'Estadounidense')
AS nuevo
ON DUPLICATE KEY UPDATE
    nombre = nuevo.nombre;

INSERT INTO fabricante_motor (fabricante_motor_id, nombre) VALUES
    (1, 'Honda'),
    (2, 'Mercedes'),
    (3, 'Ferrari'),
    (4, 'Renault')
AS nuevo
ON DUPLICATE KEY UPDATE
    nombre = nuevo.nombre;

INSERT INTO rol_piloto (codigo, etiqueta) VALUES
    ('LIDER', 'Líder'),
    ('ESCUDERO', 'Escudero')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO tipo_habilidad (codigo, etiqueta) VALUES
    ('velocidad', 'Velocidad'),
    ('consistencia', 'Consistencia'),
    ('lluvia', 'Lluvia')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO modo_conduccion (codigo, etiqueta) VALUES
    ('conduccion_normal', 'Normal'),
    ('conduccion_agresiva', 'Agresiva'),
    ('ahorro_combustible', 'Ahorro de combustible')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO condicion_climatica (codigo, etiqueta, factor_tiempo) VALUES
    ('seco', 'Seco', 1.00000),
    ('lluvioso', 'Lluvioso', 1.08000),
    ('extremo', 'Extremo', 1.18000)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    factor_tiempo = nuevo.factor_tiempo;

INSERT INTO carga_aerodinamica (codigo, etiqueta, factor_tiempo, factor_consumo, factor_desgaste) VALUES
    ('baja', 'Baja', 1.01000, 0.95000, 1.00000),
    ('media', 'Media', 1.00000, 1.00000, 1.00000),
    ('alta', 'Alta', 0.99500, 1.08000, 1.05000)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    factor_tiempo = nuevo.factor_tiempo,
    factor_consumo = nuevo.factor_consumo,
    factor_desgaste = nuevo.factor_desgaste;

INSERT INTO presion_neumatico (codigo, etiqueta, factor_tiempo, factor_desgaste) VALUES
    ('baja', 'Baja', 0.99500, 1.15000),
    ('estandar', 'Estándar', 1.00000, 1.00000),
    ('alta', 'Alta', 1.00500, 0.90000)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    factor_tiempo = nuevo.factor_tiempo,
    factor_desgaste = nuevo.factor_desgaste;

INSERT INTO estrategia_combustible (codigo, etiqueta, factor_tiempo, factor_consumo) VALUES
    ('agresiva', 'Agresiva', 0.99000, 1.15000),
    ('balanceada', 'Balanceada', 1.00000, 1.00000),
    ('ahorro', 'Ahorro', 1.01000, 0.85000)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    factor_tiempo = nuevo.factor_tiempo,
    factor_consumo = nuevo.factor_consumo;

INSERT INTO compuesto_neumatico (codigo, etiqueta, factor_tiempo, factor_desgaste) VALUES
    ('S', 'Soft', 0.98500, 1.28000),
    ('M', 'Medium', 1.00000, 1.00000),
    ('H', 'Hard', 1.01200, 0.76000)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    factor_tiempo = nuevo.factor_tiempo,
    factor_desgaste = nuevo.factor_desgaste;

INSERT INTO estado_clima_dinamico (codigo, etiqueta, condicion_codigo, factor_tiempo_base, estado_pista, neumatico_recomendado, estrategia_recomendada) VALUES
    ('SECO', 'Seco', 'seco', 1.00000, 'Pista seca', 'Slicks', 'Ataque controlado'),
    ('NUBLADO', 'Nublado', 'seco', 1.01000, 'Pista seca', 'Slicks', 'Equilibrada'),
    ('LLUVIA_LIGERA', 'Lluvia ligera', 'lluvioso', 1.04000, 'Pista húmeda', 'Intermedios', 'Conservadora'),
    ('LLUVIA', 'Lluvia', 'lluvioso', 1.08000, 'Pista mojada', 'Intermedios', 'Conservadora'),
    ('LLUVIA_INTENSA', 'Lluvia intensa', 'extremo', 1.18000, 'Pista con agua', 'Lluvia extrema', 'Máxima precaución')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    condicion_codigo = nuevo.condicion_codigo,
    factor_tiempo_base = nuevo.factor_tiempo_base,
    estado_pista = nuevo.estado_pista,
    neumatico_recomendado = nuevo.neumatico_recomendado,
    estrategia_recomendada = nuevo.estrategia_recomendada;

-- Catálogos de eventos y estados de sesión
INSERT INTO categoria_evento (codigo, etiqueta) VALUES
    ('NO_EVENT', 'Sin evento'),
    ('POSITIVE', 'Positivo'),
    ('MINOR_NEGATIVE', 'Negativo leve'),
    ('MAJOR_NEGATIVE', 'Negativo importante'),
    ('WEATHER_TRACK', 'Clima o pista'),
    ('EXCEPTIONAL', 'Excepcional')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO alcance_evento (codigo, etiqueta) VALUES
    ('INDIVIDUAL', 'Individual'),
    ('GLOBAL', 'Global')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO bandera_pista (codigo, etiqueta) VALUES
    ('GREEN', 'Pista libre'),
    ('LOCAL_YELLOW', 'Bandera amarilla local'),
    ('YELLOW', 'Bandera amarilla'),
    ('RED', 'Bandera roja')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO sector_pista (codigo, numero, etiqueta) VALUES
    ('NONE', NULL, '—'),
    ('SECTOR_1', 1, 'Sector 1'),
    ('SECTOR_2', 2, 'Sector 2'),
    ('SECTOR_3', 3, 'Sector 3')
AS nuevo
ON DUPLICATE KEY UPDATE
    numero = nuevo.numero,
    etiqueta = nuevo.etiqueta;

INSERT INTO estado_vuelta (codigo, etiqueta) VALUES
    ('VALID', 'Válida'),
    ('INVALID', 'Invalidada'),
    ('OUT', 'Fuera de sesión')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO fase_pit_stop (codigo, etiqueta, completada) VALUES
    ('ENTERING', 'Entrada a boxes', FALSE),
    ('STOPPED', 'Detenido en boxes', FALSE),
    ('EXITING', 'Salida de boxes', FALSE),
    ('COMPLETED', 'Parada completada', TRUE)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    completada = nuevo.completada;

INSERT INTO motivo_pit_stop (codigo, etiqueta) VALUES
    ('TYRE_CONDITION', 'Estado crítico de neumáticos'),
    ('WEATHER_CHANGE', 'Cambio de condiciones'),
    ('MECHANICAL_RISK', 'Riesgo mecánico'),
    ('EXCESSIVE_WEAR', 'Desgaste excesivo')
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta;

INSERT INTO tipo_evento (codigo, etiqueta, categoria_codigo, alcance_codigo, peso_base, cooldown_vueltas) VALUES
    ('NO_EVENT', 'Sin evento', 'NO_EVENT', 'INDIVIDUAL', 1, 0),
    ('PERFECT_LAP', 'Vuelta perfecta', 'POSITIVE', 'INDIVIDUAL', 1, 4),
    ('CLEAN_AIR', 'Aire limpio', 'POSITIVE', 'INDIVIDUAL', 2.1, 2),
    ('SLIPSTREAM', 'Rebufo', 'POSITIVE', 'INDIVIDUAL', 1.8, 2),
    ('TRACK_EVOLUTION_ADVANTAGE', 'Ventaja por evolución de pista', 'POSITIVE', 'INDIVIDUAL', 1.5, 3),
    ('STRONG_SECTOR', 'Sector sobresaliente', 'POSITIVE', 'INDIVIDUAL', 2.6, 1),
    ('TRAFFIC', 'Tráfico', 'MINOR_NEGATIVE', 'INDIVIDUAL', 2.5, 1),
    ('DRIVER_MISTAKE', 'Error del piloto', 'MINOR_NEGATIVE', 'INDIVIDUAL', 1.8, 2),
    ('LOCK_UP', 'Bloqueo de neumáticos', 'MINOR_NEGATIVE', 'INDIVIDUAL', 1.5, 2),
    ('WHEELSPIN', 'Patinaje de ruedas', 'MINOR_NEGATIVE', 'INDIVIDUAL', 1.6, 2),
    ('WIDE_CORNER', 'Salida amplia en curva', 'MINOR_NEGATIVE', 'INDIVIDUAL', 1.8, 2),
    ('OVERSTEER', 'Sobreviraje', 'MINOR_NEGATIVE', 'INDIVIDUAL', 1.4, 2),
    ('UNDERSTEER', 'Subviraje', 'MINOR_NEGATIVE', 'INDIVIDUAL', 1.4, 2),
    ('HEAVY_TRAFFIC', 'Tráfico intenso', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 1.5, 3),
    ('TYRE_OVERHEATING', 'Sobrecalentamiento de neumáticos', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 1.3, 3),
    ('TYRE_TOO_COLD', 'Neumáticos demasiado fríos', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 1.2, 3),
    ('BRAKE_OVERHEATING', 'Sobrecalentamiento de frenos', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 0.9, 4),
    ('ENGINE_TEMPERATURE_HIGH', 'Temperatura del motor elevada', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 0.8, 4),
    ('MINOR_MECHANICAL_ISSUE', 'Problema mecánico menor', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 0.7, 5),
    ('POWER_UNIT_DERATING', 'Reducción de potencia', 'MAJOR_NEGATIVE', 'INDIVIDUAL', 0.6, 5),
    ('YELLOW_FLAG', 'Bandera amarilla', 'WEATHER_TRACK', 'GLOBAL', 1, 4),
    ('LOCAL_YELLOW_FLAG', 'Bandera amarilla local', 'WEATHER_TRACK', 'GLOBAL', 1.8, 3),
    ('RAIN_STARTS', 'Comienza la lluvia', 'WEATHER_TRACK', 'GLOBAL', 1.4, 5),
    ('RAIN_INTENSIFIES', 'La lluvia se intensifica', 'WEATHER_TRACK', 'GLOBAL', 1.2, 4),
    ('RAIN_STOPS', 'Deja de llover', 'WEATHER_TRACK', 'GLOBAL', 1, 5),
    ('TRACK_DRYING', 'La pista se está secando', 'WEATHER_TRACK', 'GLOBAL', 1.3, 3),
    ('WIND_GUST', 'Ráfaga de viento', 'WEATHER_TRACK', 'GLOBAL', 1.7, 2),
    ('RED_FLAG', 'Bandera roja', 'EXCEPTIONAL', 'GLOBAL', 4, 10),
    ('CRASH', 'Accidente', 'EXCEPTIONAL', 'INDIVIDUAL', 1, 10)
AS nuevo
ON DUPLICATE KEY UPDATE
    etiqueta = nuevo.etiqueta,
    categoria_codigo = nuevo.categoria_codigo,
    alcance_codigo = nuevo.alcance_codigo,
    peso_base = nuevo.peso_base,
    cooldown_vueltas = nuevo.cooldown_vueltas;

-- Perfil probabilístico estándar de EventProbabilityConfig.standard()
INSERT INTO perfil_probabilidad_evento (perfil_id, nombre, probabilidad_coexistencia_global, activo) VALUES
    (1, 'Estándar', 0.0050000, TRUE)
AS nuevo
ON DUPLICATE KEY UPDATE
    nombre = nuevo.nombre,
    probabilidad_coexistencia_global = nuevo.probabilidad_coexistencia_global,
    activo = nuevo.activo;

INSERT INTO perfil_probabilidad_categoria (perfil_id, categoria_codigo, probabilidad) VALUES
    (1, 'NO_EVENT', 0.72),
    (1, 'POSITIVE', 0.08),
    (1, 'MINOR_NEGATIVE', 0.12),
    (1, 'MAJOR_NEGATIVE', 0.05),
    (1, 'WEATHER_TRACK', 0.02),
    (1, 'EXCEPTIONAL', 0.01)
AS nuevo
ON DUPLICATE KEY UPDATE
    probabilidad = nuevo.probabilidad;

-- Equipos, pilotos y habilidades del seed
INSERT INTO equipo (equipo_id, nombre, pais_id, fabricante_motor_id, imagen_url) VALUES
    (1, 'Red Bull Racing', 1, 1, 'https://upload.wikimedia.org/wikipedia/commons/b/bb/Red_Bull_Racing_Logo.svg'),
    (2, 'Mercedes-AMG Petronas', 2, 2, 'https://upload.wikimedia.org/wikipedia/commons/3/32/Mercedes_AMG_Petronas_F1_Team_logo.svg'),
    (3, 'Ferrari', 3, 3, 'https://upload.wikimedia.org/wikipedia/en/d/d4/Scuderia_Ferrari_Logo.svg'),
    (4, 'McLaren', 4, 2, NULL),
    (5, 'Aston Martin', 4, 2, NULL),
    (6, 'Alpine', 5, 4, NULL),
    (7, 'Alfa Romeo', 6, 3, NULL),
    (8, 'Haas', 7, 3, NULL),
    (9, 'AlphaTauri', 3, 1, NULL),
    (10, 'Williams', 4, 2, NULL)
AS nuevo
ON DUPLICATE KEY UPDATE
    nombre = nuevo.nombre,
    pais_id = nuevo.pais_id,
    fabricante_motor_id = nuevo.fabricante_motor_id,
    imagen_url = nuevo.imagen_url;

INSERT INTO piloto (piloto_id, equipo_id, rol_codigo, nacionalidad_id, nombre, experiencia_anios, numero, codigo_tv, victorias, campeonatos, imagen_url) VALUES
    (1, 1, 'LIDER', 1, 'Max Verstappen', 9, 1, 'VER', 59, 4, '/images/drivers/max-verstappen.jpg'),
    (2, 1, 'ESCUDERO', 2, 'Sergio Pérez', 10, 11, 'PER', 6, 0, '/images/drivers/sergio-perez.png'),
    (3, 2, 'LIDER', 3, 'Lewis Hamilton', 17, 44, 'HAM', 103, 7, '/images/drivers/lewis-hamilton.jpg'),
    (4, 2, 'ESCUDERO', 3, 'George Russell', 5, 63, 'RUS', 2, 0, '/images/drivers/george-russell.png'),
    (5, 3, 'LIDER', 4, 'Charles Leclerc', 6, 16, 'LEC', 7, 0, '/images/drivers/charles-leclerc.jpg'),
    (6, 3, 'ESCUDERO', 5, 'Carlos Sainz', 9, 55, 'SAI', 3, 0, '/images/drivers/carlos-sainz.jpg'),
    (7, 4, 'LIDER', 3, 'Lando Norris', 5, 4, 'NOR', 3, 0, '/images/drivers/lando-norris.png'),
    (8, 4, 'ESCUDERO', 6, 'Oscar Piastri', 1, 81, 'PIA', 1, 0, '/images/drivers/oscar-piastri.png'),
    (9, 5, 'LIDER', 5, 'Fernando Alonso', 20, 14, 'ALO', 32, 2, '/images/drivers/fernando-alonso.jpeg'),
    (10, 5, 'ESCUDERO', 7, 'Lance Stroll', 7, 18, 'STR', 0, 0, '/images/drivers/lance-stroll.jpg'),
    (11, 6, 'LIDER', 8, 'Esteban Ocon', 7, 31, 'OCO', 1, 0, '/images/drivers/esteban-ocon.jpeg'),
    (12, 6, 'ESCUDERO', 8, 'Pierre Gasly', 7, 10, 'GAS', 1, 0, '/images/drivers/pierre-gasly.png'),
    (13, 7, 'LIDER', 9, 'Valtteri Bottas', 11, 77, 'BOT', 10, 0, '/images/drivers/valtteri-bottas.jpeg'),
    (14, 7, 'ESCUDERO', 10, 'Zhou Guanyu', 2, 24, 'ZHO', 0, 0, '/images/drivers/zhou-guanyu.png'),
    (15, 8, 'LIDER', 11, 'Kevin Magnussen', 8, 20, 'MAG', 0, 0, '/images/drivers/kevin-magnussen.jpeg'),
    (16, 8, 'ESCUDERO', 12, 'Nico Hülkenberg', 12, 27, 'HUL', 0, 0, '/images/drivers/nico-hulkenberg.png'),
    (17, 9, 'LIDER', 13, 'Yuki Tsunoda', 4, 22, 'TSU', 0, 0, '/images/drivers/yuki-tsunoda.jpeg'),
    (18, 9, 'ESCUDERO', 6, 'Daniel Ricciardo', 13, 3, 'RIC', 8, 0, '/images/drivers/daniel-ricciardo.jpg'),
    (19, 10, 'LIDER', 14, 'Alexander Albon', 5, 23, 'ALB', 0, 0, '/images/drivers/alexander-albon.png'),
    (20, 10, 'ESCUDERO', 15, 'Logan Sargeant', 1, 2, 'SAR', 0, 0, '/images/drivers/logan-sargeant.png')
AS nuevo
ON DUPLICATE KEY UPDATE
    equipo_id = nuevo.equipo_id,
    rol_codigo = nuevo.rol_codigo,
    nacionalidad_id = nuevo.nacionalidad_id,
    nombre = nuevo.nombre,
    experiencia_anios = nuevo.experiencia_anios,
    numero = nuevo.numero,
    codigo_tv = nuevo.codigo_tv,
    victorias = nuevo.victorias,
    campeonatos = nuevo.campeonatos,
    imagen_url = nuevo.imagen_url;

INSERT INTO piloto_habilidad (piloto_id, habilidad_codigo, valor) VALUES
    (1, 'velocidad', 98),
    (1, 'consistencia', 95),
    (1, 'lluvia', 96),
    (2, 'velocidad', 88),
    (2, 'consistencia', 80),
    (2, 'lluvia', 82),
    (3, 'velocidad', 95),
    (3, 'consistencia', 93),
    (3, 'lluvia', 97),
    (4, 'velocidad', 90),
    (4, 'consistencia', 88),
    (4, 'lluvia', 86),
    (5, 'velocidad', 94),
    (5, 'consistencia', 85),
    (5, 'lluvia', 88),
    (6, 'velocidad', 89),
    (6, 'consistencia', 90),
    (6, 'lluvia', 85),
    (7, 'velocidad', 92),
    (7, 'consistencia', 89),
    (7, 'lluvia', 90),
    (8, 'velocidad', 87),
    (8, 'consistencia', 84),
    (8, 'lluvia', 80),
    (9, 'velocidad', 93),
    (9, 'consistencia', 94),
    (9, 'lluvia', 95),
    (10, 'velocidad', 80),
    (10, 'consistencia', 75),
    (10, 'lluvia', 78),
    (11, 'velocidad', 84),
    (11, 'consistencia', 82),
    (11, 'lluvia', 83),
    (12, 'velocidad', 85),
    (12, 'consistencia', 81),
    (12, 'lluvia', 84),
    (13, 'velocidad', 86),
    (13, 'consistencia', 87),
    (13, 'lluvia', 84),
    (14, 'velocidad', 78),
    (14, 'consistencia', 76),
    (14, 'lluvia', 74),
    (15, 'velocidad', 82),
    (15, 'consistencia', 74),
    (15, 'lluvia', 79),
    (16, 'velocidad', 84),
    (16, 'consistencia', 83),
    (16, 'lluvia', 86),
    (17, 'velocidad', 83),
    (17, 'consistencia', 75),
    (17, 'lluvia', 77),
    (18, 'velocidad', 86),
    (18, 'consistencia', 82),
    (18, 'lluvia', 85),
    (19, 'velocidad', 85),
    (19, 'consistencia', 84),
    (19, 'lluvia', 82),
    (20, 'velocidad', 74),
    (20, 'consistencia', 70),
    (20, 'lluvia', 68)
AS nuevo
ON DUPLICATE KEY UPDATE
    valor = nuevo.valor;

-- Vehículos y rendimientos multivaluados separados en tablas 4FN
INSERT INTO vehiculo (vehiculo_id, equipo_id, fabricante_motor_id, modelo, velocidad_maxima_kmh, aceleracion_0_100, imagen_url) VALUES
    (1, 1, 1, 'RB20', 360, 2.5, '/images/vehicles/RB20/principal.jpg'),
    (2, 2, 2, 'W15', 355, 2.6, '/images/vehicles/W15/principal.jpg'),
    (3, 3, 3, 'SF-24', 357, 2.5, '/images/vehicles/SF-24/principal.jpg'),
    (4, 4, 2, 'MCL38', 356, 2.6, '/images/vehicles/MCL38/principal.jpg'),
    (5, 5, 2, 'AMR24', 352, 2.7, '/images/vehicles/AMR24/principal.jpg'),
    (6, 9, 1, 'AT04', 349, 2.8, '/images/vehicles/AT04/principal.jpg'),
    (7, 6, 4, 'A524', 350, 2.8, '/images/vehicles/A524/principal.jpg'),
    (8, 7, 3, 'C43', 348, 2.8, '/images/vehicles/C43/principal.jpg'),
    (9, 8, 3, 'VF-24', 347, 2.9, '/images/vehicles/VF-24/principal.jpg'),
    (10, 10, 2, 'FW46', 346, 2.9, '/images/vehicles/FW46/principal.jpg')
AS nuevo
ON DUPLICATE KEY UPDATE
    equipo_id = nuevo.equipo_id,
    fabricante_motor_id = nuevo.fabricante_motor_id,
    modelo = nuevo.modelo,
    velocidad_maxima_kmh = nuevo.velocidad_maxima_kmh,
    aceleracion_0_100 = nuevo.aceleracion_0_100,
    imagen_url = nuevo.imagen_url;

INSERT INTO vehiculo_piloto (vehiculo_id, piloto_id) VALUES
    (1, 1),
    (1, 2),
    (2, 3),
    (2, 4),
    (3, 5),
    (3, 6),
    (4, 7),
    (4, 8),
    (5, 9),
    (5, 10),
    (6, 17),
    (6, 18),
    (7, 11),
    (7, 12),
    (8, 13),
    (8, 14),
    (9, 15),
    (9, 16),
    (10, 19),
    (10, 20)
AS nuevo
ON DUPLICATE KEY UPDATE
    piloto_id = nuevo.piloto_id;

INSERT INTO vehiculo_rendimiento (vehiculo_id, modo_codigo, velocidad_promedio_kmh) VALUES
    (1, 'conduccion_normal', 320),
    (1, 'conduccion_agresiva', 340),
    (1, 'ahorro_combustible', 300),
    (2, 'conduccion_normal', 315),
    (2, 'conduccion_agresiva', 335),
    (2, 'ahorro_combustible', 295),
    (3, 'conduccion_normal', 317),
    (3, 'conduccion_agresiva', 337),
    (3, 'ahorro_combustible', 297),
    (4, 'conduccion_normal', 316),
    (4, 'conduccion_agresiva', 336),
    (4, 'ahorro_combustible', 296),
    (5, 'conduccion_normal', 312),
    (5, 'conduccion_agresiva', 332),
    (5, 'ahorro_combustible', 292),
    (6, 'conduccion_normal', 309),
    (6, 'conduccion_agresiva', 329),
    (6, 'ahorro_combustible', 289),
    (7, 'conduccion_normal', 310),
    (7, 'conduccion_agresiva', 330),
    (7, 'ahorro_combustible', 290),
    (8, 'conduccion_normal', 308),
    (8, 'conduccion_agresiva', 328),
    (8, 'ahorro_combustible', 288),
    (9, 'conduccion_normal', 307),
    (9, 'conduccion_agresiva', 327),
    (9, 'ahorro_combustible', 287),
    (10, 'conduccion_normal', 306),
    (10, 'conduccion_agresiva', 326),
    (10, 'ahorro_combustible', 286)
AS nuevo
ON DUPLICATE KEY UPDATE
    velocidad_promedio_kmh = nuevo.velocidad_promedio_kmh;

INSERT INTO vehiculo_rendimiento_clima (vehiculo_id, modo_codigo, condicion_codigo, consumo_combustible, desgaste_neumaticos) VALUES
    (1, 'conduccion_normal', 'seco', 1.9, 1.5),
    (1, 'conduccion_normal', 'lluvioso', 2.1, 0.8),
    (1, 'conduccion_normal', 'extremo', 2.4, 2.5),
    (1, 'conduccion_agresiva', 'seco', 2.4, 2.2),
    (1, 'conduccion_agresiva', 'lluvioso', 2.6, 1.2),
    (1, 'conduccion_agresiva', 'extremo', 3, 3.5),
    (1, 'ahorro_combustible', 'seco', 1.6, 1),
    (1, 'ahorro_combustible', 'lluvioso', 1.8, 0.5),
    (1, 'ahorro_combustible', 'extremo', 2.1, 1.8),
    (2, 'conduccion_normal', 'seco', 2, 1.6),
    (2, 'conduccion_normal', 'lluvioso', 2.2, 0.9),
    (2, 'conduccion_normal', 'extremo', 2.5, 2.6),
    (2, 'conduccion_agresiva', 'seco', 2.6, 2.3),
    (2, 'conduccion_agresiva', 'lluvioso', 2.8, 1.4),
    (2, 'conduccion_agresiva', 'extremo', 3.2, 3.8),
    (2, 'ahorro_combustible', 'seco', 1.7, 1.1),
    (2, 'ahorro_combustible', 'lluvioso', 1.9, 0.6),
    (2, 'ahorro_combustible', 'extremo', 2.2, 1.9),
    (3, 'conduccion_normal', 'seco', 1.9, 1.5),
    (3, 'conduccion_normal', 'lluvioso', 2.1, 0.8),
    (3, 'conduccion_normal', 'extremo', 2.4, 2.5),
    (3, 'conduccion_agresiva', 'seco', 2.4, 2.2),
    (3, 'conduccion_agresiva', 'lluvioso', 2.7, 1.2),
    (3, 'conduccion_agresiva', 'extremo', 3.1, 3.6),
    (3, 'ahorro_combustible', 'seco', 1.6, 1),
    (3, 'ahorro_combustible', 'lluvioso', 1.8, 0.5),
    (3, 'ahorro_combustible', 'extremo', 2.1, 1.8),
    (4, 'conduccion_normal', 'seco', 2, 1.5),
    (4, 'conduccion_normal', 'lluvioso', 2.2, 0.8),
    (4, 'conduccion_normal', 'extremo', 2.5, 2.6),
    (4, 'conduccion_agresiva', 'seco', 2.5, 2.3),
    (4, 'conduccion_agresiva', 'lluvioso', 2.7, 1.2),
    (4, 'conduccion_agresiva', 'extremo', 3.1, 3.6),
    (4, 'ahorro_combustible', 'seco', 1.6, 1),
    (4, 'ahorro_combustible', 'lluvioso', 1.9, 0.5),
    (4, 'ahorro_combustible', 'extremo', 2.2, 1.9),
    (5, 'conduccion_normal', 'seco', 2, 1.6),
    (5, 'conduccion_normal', 'lluvioso', 2.2, 0.9),
    (5, 'conduccion_normal', 'extremo', 2.6, 2.7),
    (5, 'conduccion_agresiva', 'seco', 2.6, 2.4),
    (5, 'conduccion_agresiva', 'lluvioso', 2.8, 1.3),
    (5, 'conduccion_agresiva', 'extremo', 3.2, 3.7),
    (5, 'ahorro_combustible', 'seco', 1.7, 1.1),
    (5, 'ahorro_combustible', 'lluvioso', 1.9, 0.5),
    (5, 'ahorro_combustible', 'extremo', 2.2, 1.9),
    (6, 'conduccion_normal', 'seco', 2.1, 1.6),
    (6, 'conduccion_normal', 'lluvioso', 2.3, 0.9),
    (6, 'conduccion_normal', 'extremo', 2.6, 2.7),
    (6, 'conduccion_agresiva', 'seco', 2.6, 2.4),
    (6, 'conduccion_agresiva', 'lluvioso', 2.8, 1.3),
    (6, 'conduccion_agresiva', 'extremo', 3.2, 3.8),
    (6, 'ahorro_combustible', 'seco', 1.7, 1.1),
    (6, 'ahorro_combustible', 'lluvioso', 1.9, 0.5),
    (6, 'ahorro_combustible', 'extremo', 2.3, 1.9),
    (7, 'conduccion_normal', 'seco', 2.1, 1.6),
    (7, 'conduccion_normal', 'lluvioso', 2.3, 0.9),
    (7, 'conduccion_normal', 'extremo', 2.6, 2.7),
    (7, 'conduccion_agresiva', 'seco', 2.6, 2.4),
    (7, 'conduccion_agresiva', 'lluvioso', 2.8, 1.3),
    (7, 'conduccion_agresiva', 'extremo', 3.3, 3.8),
    (7, 'ahorro_combustible', 'seco', 1.7, 1.1),
    (7, 'ahorro_combustible', 'lluvioso', 2, 0.5),
    (7, 'ahorro_combustible', 'extremo', 2.3, 2),
    (8, 'conduccion_normal', 'seco', 2.1, 1.7),
    (8, 'conduccion_normal', 'lluvioso', 2.3, 0.9),
    (8, 'conduccion_normal', 'extremo', 2.6, 2.8),
    (8, 'conduccion_agresiva', 'seco', 2.6, 2.4),
    (8, 'conduccion_agresiva', 'lluvioso', 2.9, 1.3),
    (8, 'conduccion_agresiva', 'extremo', 3.3, 3.9),
    (8, 'ahorro_combustible', 'seco', 1.8, 1.1),
    (8, 'ahorro_combustible', 'lluvioso', 2, 0.6),
    (8, 'ahorro_combustible', 'extremo', 2.3, 2),
    (9, 'conduccion_normal', 'seco', 2.1, 1.7),
    (9, 'conduccion_normal', 'lluvioso', 2.3, 0.9),
    (9, 'conduccion_normal', 'extremo', 2.7, 2.8),
    (9, 'conduccion_agresiva', 'seco', 2.7, 2.4),
    (9, 'conduccion_agresiva', 'lluvioso', 2.9, 1.3),
    (9, 'conduccion_agresiva', 'extremo', 3.3, 3.9),
    (9, 'ahorro_combustible', 'seco', 1.8, 1.1),
    (9, 'ahorro_combustible', 'lluvioso', 2, 0.6),
    (9, 'ahorro_combustible', 'extremo', 2.3, 2),
    (10, 'conduccion_normal', 'seco', 2.1, 1.7),
    (10, 'conduccion_normal', 'lluvioso', 2.4, 0.9),
    (10, 'conduccion_normal', 'extremo', 2.7, 2.8),
    (10, 'conduccion_agresiva', 'seco', 2.7, 2.5),
    (10, 'conduccion_agresiva', 'lluvioso', 2.9, 1.3),
    (10, 'conduccion_agresiva', 'extremo', 3.4, 3.9),
    (10, 'ahorro_combustible', 'seco', 1.8, 1.1),
    (10, 'ahorro_combustible', 'lluvioso', 2, 0.6),
    (10, 'ahorro_combustible', 'extremo', 2.4, 2)
AS nuevo
ON DUPLICATE KEY UPDATE
    consumo_combustible = nuevo.consumo_combustible,
    desgaste_neumaticos = nuevo.desgaste_neumaticos;

-- Circuitos y sus relaciones independientes
INSERT INTO circuito (circuito_id, pais_id, nombre, longitud_km, vueltas, descripcion, factor_tecnico, factor_consumo, factor_desgaste, imagen_url) VALUES
    (1, 8, 'Circuito de Mónaco', 3.34, 78, 'Uno de los circuitos más prestigiosos y difíciles del calendario, conocido por sus calles angostas y la falta de zonas de adelantamiento.', 1.984, 0.9, 0.85, '/images/circuits/monaco.png'),
    (2, 4, 'Silverstone', 5.89, 52, 'Uno de los circuitos más rápidos del calendario, con curvas de alta velocidad como Maggotts y Becketts.', 1.397, 1.1, 1.2, '/images/circuits/silverstone.jpg'),
    (3, 9, 'Circuito de Spa-Francorchamps', 7, 44, 'Famoso por la curva Eau Rouge y la larga recta de Kemmel, un circuito donde la potencia del motor es clave.', 1.434, 1.12, 1.1, '/images/circuits/spa-francorchamps.png'),
    (4, 3, 'Circuito de Monza', 5.79, 53, 'Conocido como ''El Templo de la Velocidad'', Monza es el circuito más rápido del calendario con largas rectas y chicanes icónicas.', 1.322, 1.15, 1, '/images/circuits/monza.png'),
    (5, 10, 'Interlagos', 4.31, 71, 'Interlagos es un circuito legendario con cambios de elevación y un trazado técnico que ha sido sede de algunas de las carreras más emocionantes de la historia.', 1.546, 1.02, 1.05, '/images/circuits/interlagos.png'),
    (6, 11, 'Circuito de Yas Marina', 5.28, 58, 'Ubicado en Abu Dhabi, es famoso por ser el circuito donde se definen muchos campeonatos, con un diseño moderno y una espectacular carrera nocturna.', 1.776, 0.98, 0.95, '/images/circuits/yas-marina.png'),
    (7, 12, 'Circuito de Suzuka', 5.81, 53, 'Un circuito desafiante con un diseño en forma de ocho, famoso por sus curvas de alta velocidad como 130R y la ''S'' de Senna.', 1.479, 1.05, 1.15, '/images/circuits/suzuka.jpg')
AS nuevo
ON DUPLICATE KEY UPDATE
    pais_id = nuevo.pais_id,
    nombre = nuevo.nombre,
    longitud_km = nuevo.longitud_km,
    vueltas = nuevo.vueltas,
    descripcion = nuevo.descripcion,
    factor_tecnico = nuevo.factor_tecnico,
    factor_consumo = nuevo.factor_consumo,
    factor_desgaste = nuevo.factor_desgaste,
    imagen_url = nuevo.imagen_url;

INSERT INTO circuito_record_vuelta (circuito_id, tiempo_segundos, titular_nombre, anio) VALUES
    (1, 70.166, 'Lewis Hamilton', 2019),
    (2, 87.097, 'Max Verstappen', 2020),
    (3, 106.286, 'Valtteri Bottas', 2018),
    (4, 81.046, 'Rubens Barrichello', 2004),
    (5, 70.540, 'Valtteri Bottas', 2018),
    (6, 99.283, 'Lewis Hamilton', 2019),
    (7, 90.983, 'Lewis Hamilton', 2019)
AS nuevo
ON DUPLICATE KEY UPDATE
    tiempo_segundos = nuevo.tiempo_segundos,
    titular_nombre = nuevo.titular_nombre,
    anio = nuevo.anio;

INSERT INTO circuito_probabilidad_clima (circuito_id, condicion_codigo, probabilidad) VALUES
    (1, 'seco', 0.75),
    (1, 'lluvioso', 0.2),
    (1, 'extremo', 0.05),
    (2, 'seco', 0.55),
    (2, 'lluvioso', 0.35),
    (2, 'extremo', 0.1),
    (3, 'seco', 0.5),
    (3, 'lluvioso', 0.35),
    (3, 'extremo', 0.15),
    (4, 'seco', 0.8),
    (4, 'lluvioso', 0.15),
    (4, 'extremo', 0.05),
    (5, 'seco', 0.6),
    (5, 'lluvioso', 0.3),
    (5, 'extremo', 0.1),
    (6, 'seco', 0.95),
    (6, 'lluvioso', 0.04),
    (6, 'extremo', 0.01),
    (7, 'seco', 0.65),
    (7, 'lluvioso', 0.25),
    (7, 'extremo', 0.1)
AS nuevo
ON DUPLICATE KEY UPDATE
    probabilidad = nuevo.probabilidad;

INSERT INTO circuito_ganador (circuito_id, temporada, piloto_id) VALUES
    (1, 2021, 1),
    (1, 2022, 2),
    (1, 2023, 1),
    (2, 2021, 3),
    (2, 2022, 5),
    (2, 2023, 1),
    (3, 2021, 1),
    (3, 2022, 1),
    (3, 2023, 1),
    (4, 2021, 2),
    (4, 2022, 1),
    (4, 2023, 1),
    (5, 2021, 3),
    (5, 2022, 1),
    (5, 2023, 1),
    (6, 2021, 1),
    (6, 2022, 1),
    (6, 2023, 3),
    (7, 2021, 1),
    (7, 2022, 1),
    (7, 2023, 1)
AS nuevo
ON DUPLICATE KEY UPDATE
    piloto_id = nuevo.piloto_id;

-- Fichas ampliadas generadas desde seed.json
UPDATE equipo SET nombre_completo = 'Oracle Red Bull Racing', base = 'Milton Keynes, Reino Unido', jefe_equipo = 'Christian Horner', jefe_tecnico = 'Pierre Waché', piloto_reserva = 'Liam Lawson', primera_participacion = 2005, campeonatos = 6, gran_premios = 379, victorias = 118, podios = 285, poles = 103, descripcion = 'Nacida de la compra de Jaguar en 2005, tardó cinco años en ganar y desde entonces ha dominado dos eras distintas del reglamento. Su fábrica de Milton Keynes es hoy la referencia en aerodinámica de la parrilla.' WHERE nombre = 'Red Bull Racing';
UPDATE equipo SET nombre_completo = 'Mercedes-AMG PETRONAS Formula One Team', base = 'Brackley, Reino Unido', jefe_equipo = 'Toto Wolff', jefe_tecnico = 'James Allison', piloto_reserva = 'Frederik Vesti', primera_participacion = 1970, campeonatos = 8, gran_premios = 285, victorias = 125, podios = 287, poles = 137, descripcion = 'La escudería que convirtió la llegada de los motores híbridos en ocho títulos de constructores consecutivos. Fabrica su propia unidad de potencia en Brixworth y la suministra a media parrilla.' WHERE nombre = 'Mercedes-AMG Petronas';
UPDATE equipo SET nombre_completo = 'Scuderia Ferrari', base = 'Maranello, Italia', jefe_equipo = 'Frédéric Vasseur', jefe_tecnico = 'Enrico Cardile', piloto_reserva = 'Antonio Giovinazzi', primera_participacion = 1950, campeonatos = 16, gran_premios = 1102, victorias = 246, podios = 819, poles = 253, descripcion = 'El único equipo que ha estado en la parrilla desde el primer campeonato de 1950. Diseña y construye chasis y motor en Maranello, y acumula más victorias, podios y poles que ninguna otra escudería.' WHERE nombre = 'Ferrari';
UPDATE equipo SET nombre_completo = 'McLaren Formula 1 Team', base = 'Woking, Reino Unido', jefe_equipo = 'Andrea Stella', jefe_tecnico = 'Peter Prodromou', piloto_reserva = 'Ryo Hirakawa', primera_participacion = 1966, campeonatos = 9, gran_premios = 976, victorias = 189, podios = 534, poles = 165, descripcion = 'Fundada por el piloto neozelandés Bruce McLaren, es la segunda más laureada de la historia. Su túnel de viento propio en Woking marcó el regreso al podio tras una larga travesía.' WHERE nombre = 'McLaren';
UPDATE equipo SET nombre_completo = 'Aston Martin Aramco Formula One Team', base = 'Silverstone, Reino Unido', jefe_equipo = 'Mike Krack', jefe_tecnico = 'Dan Fallows', piloto_reserva = 'Felipe Drugovich', primera_participacion = 2021, campeonatos = 0, gran_premios = 92, victorias = 0, podios = 9, poles = 0, descripcion = 'El regreso de la marca británica a la F1 tras seis décadas, sobre la estructura de Racing Point. Su nueva fábrica junto al circuito de Silverstone es la más moderna de la parrilla.' WHERE nombre = 'Aston Martin';
UPDATE equipo SET nombre_completo = 'BWT Alpine F1 Team', base = 'Enstone, Reino Unido', jefe_equipo = 'Bruno Famin', jefe_tecnico = 'Matt Harman', piloto_reserva = 'Jack Doohan', primera_participacion = 2021, campeonatos = 0, gran_premios = 92, victorias = 1, podios = 3, poles = 0, descripcion = 'La marca deportiva de Renault tomó el relevo del equipo de Enstone en 2021. Es el único constructor de la parrilla con chasis en Inglaterra y motor en Francia, en Viry-Châtillon.' WHERE nombre = 'Alpine';
UPDATE equipo SET nombre_completo = 'Alfa Romeo F1 Team Stake', base = 'Hinwil, Suiza', jefe_equipo = 'Alessandro Alunni Bravi', jefe_tecnico = 'James Key', piloto_reserva = 'Theo Pourchaire', primera_participacion = 1993, campeonatos = 0, gran_premios = 560, victorias = 1, podios = 27, poles = 1, descripcion = 'La estructura de Hinwil, históricamente Sauber, corre bajo licencia de Alfa Romeo. Su túnel de viento suizo es uno de los pocos que quedan en propiedad de un equipo mediano.' WHERE nombre = 'Alfa Romeo';
UPDATE equipo SET nombre_completo = 'MoneyGram Haas F1 Team', base = 'Kannapolis, Estados Unidos', jefe_equipo = 'Ayao Komatsu', jefe_tecnico = 'Simone Resta', piloto_reserva = 'Pietro Fittipaldi', primera_participacion = 2016, campeonatos = 0, gran_premios = 179, victorias = 0, podios = 0, poles = 1, descripcion = 'El primer equipo estadounidense en tres décadas y el más pequeño de la parrilla: compra a Ferrari todo lo que el reglamento permite comprar y reparte su operación entre Carolina del Norte, Banbury y Maranello.' WHERE nombre = 'Haas';
UPDATE equipo SET nombre_completo = 'Scuderia AlphaTauri', base = 'Faenza, Italia', jefe_equipo = 'Laurent Mekies', jefe_tecnico = 'Jody Egginton', piloto_reserva = 'Liam Lawson', primera_participacion = 2006, campeonatos = 0, gran_premios = 360, victorias = 2, podios = 5, poles = 1, descripcion = 'El segundo equipo de Red Bull, antes Toro Rosso, es la escuela por la que pasan sus jóvenes promesas. Sus dos victorias llegaron en Monza, con catorce años de diferencia.' WHERE nombre = 'AlphaTauri';
UPDATE equipo SET nombre_completo = 'Williams Racing', base = 'Grove, Reino Unido', jefe_equipo = 'James Vowles', jefe_tecnico = 'Pat Fry', piloto_reserva = 'Franco Colapinto', primera_participacion = 1977, campeonatos = 9, gran_premios = 800, victorias = 114, podios = 313, poles = 128, descripcion = 'La escudería de Frank Williams dominó los ochenta y los noventa con nueve títulos de constructores. Tras años en la cola de la parrilla, reconstruye su estructura técnica desde Grove.' WHERE nombre = 'Williams';
UPDATE piloto SET fecha_nacimiento = '30/09/1997', lugar_nacimiento = 'Hasselt, Bélgica', biografia = 'Hijo del piloto Jos Verstappen, debutó en F1 con 17 años y se convirtió en el ganador más joven de un Gran Premio apenas un año después. Desde entonces ha dominado la parrilla con una combinación de agresividad al límite y una constancia que pocos igualan.' WHERE piloto_id = 1;
UPDATE piloto SET fecha_nacimiento = '26/01/1990', lugar_nacimiento = 'Guadalajara, México', biografia = 'El primer mexicano en subir al podio de la F1 moderna, escaló desde equipos modestos a base de resultados constantes hasta ganarse un asiento en el equipo de cabeza. Su fortaleza está en administrar el neumático y sacar el máximo cuando la carrera se complica.' WHERE piloto_id = 2;
UPDATE piloto SET fecha_nacimiento = '07/01/1985', lugar_nacimiento = 'Stevenage, Inglaterra', biografia = 'Llegó a la F1 en 2007 tras ser el primer piloto negro en la categoría, y desde entonces ha igualado el récord de siete campeonatos del mundo. Su lectura de carrera y su capacidad para rendir bajo lluvia lo han convertido en referencia de toda una generación.' WHERE piloto_id = 3;
UPDATE piloto SET fecha_nacimiento = '15/02/1998', lugar_nacimiento = 'King''s Lynn, Inglaterra', biografia = 'Campeón de GP3 y Fórmula 2 en años consecutivos, se ganó fama de metódico y veloz antes incluso de debutar en F1. Su vuelta sustituyendo a Hamilton en Sakhir 2020, a punto de ganar con un coche que apenas conocía, confirmó lo que ya se sospechaba.' WHERE piloto_id = 4;
UPDATE piloto SET fecha_nacimiento = '16/10/1997', lugar_nacimiento = 'Mónaco', biografia = 'Ahijado de Jules Bianchi y campeón de F2 en su única temporada en la categoría, debutó en Ferrari con apenas 21 años. Corre en casa cada vez que compite en el Gran Premio de Mónaco, la carrera con la que sueña cerrar el círculo desde niño.' WHERE piloto_id = 5;
UPDATE piloto SET fecha_nacimiento = '01/09/1994', lugar_nacimiento = 'Madrid, España', biografia = 'Hijo del bicampeón mundial de rally Carlos Sainz, se forjó una reputación de piloto sólido y sin dramas que va sumando podios equipo tras equipo. Fue el primero en cortar la racha ganadora de Red Bull en 2024, con victoria en Australia apenas semanas después de una operación de apendicitis.' WHERE piloto_id = 6;
UPDATE piloto SET fecha_nacimiento = '13/11/1999', lugar_nacimiento = 'Bristol, Inglaterra', biografia = 'Llegó a la parrilla como uno de los pilotos más precoces del karting europeo y ha crecido junto a McLaren hasta pelear el título. Su cercanía con la afición, dentro y fuera de la pista, lo ha convertido en uno de los pilotos más populares de su generación.' WHERE piloto_id = 7;
UPDATE piloto SET fecha_nacimiento = '06/04/2001', lugar_nacimiento = 'Melbourne, Australia', biografia = 'Campeón de F3, F2 y de la Fórmula Renault Eurocup en años sucesivos, llegó a la F1 con la etiqueta de mayor promesa junior del continente. Ganó su primer Gran Premio a los pocos meses de debutar, con una gestión de neumáticos impropia de un novato.' WHERE piloto_id = 8;
UPDATE piloto SET fecha_nacimiento = '29/07/1981', lugar_nacimiento = 'Oviedo, España', biografia = 'Dos veces campeón del mundo con Renault, es el piloto en activo con más experiencia de la parrilla tras más de dos décadas compitiendo. Su instinto de carrera y su capacidad para exprimir un coche por encima de sus posibilidades siguen intactos.' WHERE piloto_id = 9;
UPDATE piloto SET fecha_nacimiento = '29/10/1998', lugar_nacimiento = 'Montreal, Canadá', biografia = 'Debutó en F1 con 18 años y una victoria bajo lluvia en Bakú en su segunda temporada, todavía como uno de los más jóvenes en lograrlo. Compite para el equipo de su propia familia, lo que le exige demostrar cada fin de semana que su asiento se lo ha ganado en pista.' WHERE piloto_id = 10;
UPDATE piloto SET fecha_nacimiento = '17/09/1996', lugar_nacimiento = 'Évreux, Francia', biografia = 'Formado en la academia junior de Mercedes, tuvo que abrirse paso él solo tras quedarse sin equipo un año antes de volver más fuerte. Su primera victoria llegó en una carrera caótica en Hungría, gestionando el ritmo con una calma que contrasta con su fama de competitivo hasta el límite.' WHERE piloto_id = 11;
UPDATE piloto SET fecha_nacimiento = '07/02/1996', lugar_nacimiento = 'Ruan, Francia', biografia = 'Pasó por un año difícil en el equipo de cabeza antes de encontrar su sitio y ganar en Monza, en una carrera marcada por el respeto a la memoria de un excompañero. Desde entonces se ha consolidado como un piloto capaz de sacar resultados de coches que no siempre acompañan.' WHERE piloto_id = 12;
UPDATE piloto SET fecha_nacimiento = '28/08/1989', lugar_nacimiento = 'Nastola, Finlandia', biografia = 'Compañero de Hamilton durante cinco temporadas en el equipo de cabeza, fue una pieza clave de varios campeonatos de constructores sin perseguir jamás el protagonismo. Conocido por su temple casi imperturbable, encontró en la fotografía y el triatlón sus otras dos pasiones fuera de la pista.' WHERE piloto_id = 13;
UPDATE piloto SET fecha_nacimiento = '30/05/1999', lugar_nacimiento = 'Shanghái, China', biografia = 'Primer piloto chino en llegar a la Fórmula 1, se formó en las categorías inferiores europeas antes de dar el salto. Su debut abrió la puerta a un mercado nuevo para la categoría y consolidó una carrera hecha a base de constancia más que de golpes de efecto.' WHERE piloto_id = 14;
UPDATE piloto SET fecha_nacimiento = '05/10/1992', lugar_nacimiento = 'Roskilde, Dinamarca', biografia = 'Hijo del piloto Jan Magnussen, debutó en McLaren con un podio en su primera carrera y desde entonces ha hecho carrera de la contundencia en pista. Su estilo directo, sin miedo al roce, lo convirtió en un especialista defendiendo posiciones con equipos modestos.' WHERE piloto_id = 15;
UPDATE piloto SET fecha_nacimiento = '19/08/1987', lugar_nacimiento = 'Emmerich am Rhein, Alemania', biografia = 'Campeón de GP2 en su primer año en la categoría, es célebre por ostentar el récord de más grandes premios disputados sin subir jamás al podio. Piloto de referencia para probar coches y analizar datos, su experiencia lo mantiene competitivo temporada tras temporada.' WHERE piloto_id = 16;
UPDATE piloto SET fecha_nacimiento = '11/05/2000', lugar_nacimiento = 'Sagamihara, Japón', biografia = 'Formado en la academia junior de Honda y ganador de la Fórmula 4 japonesa antes de mudarse a Europa, llegó a la F1 con apenas un año en monoplazas de ala. Su ritmo en clasificación y su temperamento explosivo lo han convertido en uno de los pilotos más queridos por la afición japonesa.' WHERE piloto_id = 17;
UPDATE piloto SET fecha_nacimiento = '01/07/1989', lugar_nacimiento = 'Perth, Australia', biografia = 'Célebre por su sonrisa y su «Shoey» tras cada victoria, construyó su reputación adelantando por fuera en las frenadas más improbables de la parrilla. Ganó siete grandes premios entre Red Bull y McLaren antes de convertirse en uno de los pilotos más queridos por el paddock.' WHERE piloto_id = 18;
UPDATE piloto SET fecha_nacimiento = '23/03/1996', lugar_nacimiento = 'Londres, Inglaterra', biografia = 'De madre tailandesa y padre británico, compite bajo la bandera de Tailandia tras un ascenso meteórico desde la Fórmula 2 hasta un equipo de cabeza en apenas dos años. Una lesión de apendicitis no le impidió firmar una de las mejores clasificaciones de su carrera pocos días después de operarse.' WHERE piloto_id = 19;
UPDATE piloto SET fecha_nacimiento = '31/12/2000', lugar_nacimiento = 'Fort Lauderdale, Estados Unidos', biografia = 'Campeón de la Fórmula 3 estadounidense antes de mudarse a Europa, es el primer piloto de su país en la parrilla en más de una década. Su temporada de debut, sin apenas kilómetros de rodaje previos, se centró en aprender los circuitos que sus rivales ya dominaban de memoria.' WHERE piloto_id = 20;
-- Fin de fichas ampliadas

-- Las vistas de control deben devolver cero filas tras esta carga.
COMMIT;

SELECT * FROM vw_probabilidad_clima_invalida;
SELECT * FROM vw_perfil_evento_invalido;

