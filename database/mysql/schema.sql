-- Formula1Simulator - MySQL 8.0.19+
-- DDL normalizado hasta 4FN. No usa columnas JSON para ocultar relaciones.
-- Ejecutar primero con una cuenta autorizada para crear esquemas.

CREATE DATABASE IF NOT EXISTS formula1_simulator
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE formula1_simulator;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Catalogos
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pais (
    pais_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    CONSTRAINT uq_pais_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS nacionalidad (
    nacionalidad_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    CONSTRAINT uq_nacionalidad_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS fabricante_motor (
    fabricante_motor_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    CONSTRAINT uq_fabricante_motor_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS rol_piloto (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(40) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tipo_habilidad (
    codigo VARCHAR(24) PRIMARY KEY,
    etiqueta VARCHAR(50) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS modo_conduccion (
    codigo VARCHAR(32) PRIMARY KEY,
    etiqueta VARCHAR(50) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS condicion_climatica (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(40) NOT NULL UNIQUE,
    factor_tiempo DECIMAL(8,5) NOT NULL,
    CONSTRAINT chk_condicion_factor_tiempo CHECK (factor_tiempo > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS carga_aerodinamica (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(40) NOT NULL UNIQUE,
    factor_tiempo DECIMAL(8,5) NOT NULL,
    factor_consumo DECIMAL(8,5) NOT NULL,
    factor_desgaste DECIMAL(8,5) NOT NULL,
    CONSTRAINT chk_aero_factores CHECK (
        factor_tiempo > 0 AND factor_consumo > 0 AND factor_desgaste > 0
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS presion_neumatico (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(40) NOT NULL UNIQUE,
    factor_tiempo DECIMAL(8,5) NOT NULL,
    factor_desgaste DECIMAL(8,5) NOT NULL,
    CONSTRAINT chk_presion_factores CHECK (factor_tiempo > 0 AND factor_desgaste > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS estrategia_combustible (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(40) NOT NULL UNIQUE,
    factor_tiempo DECIMAL(8,5) NOT NULL,
    factor_consumo DECIMAL(8,5) NOT NULL,
    CONSTRAINT chk_combustible_factores CHECK (factor_tiempo > 0 AND factor_consumo > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS compuesto_neumatico (
    codigo CHAR(1) PRIMARY KEY,
    etiqueta VARCHAR(20) NOT NULL UNIQUE,
    factor_tiempo DECIMAL(8,5) NOT NULL,
    factor_desgaste DECIMAL(8,5) NOT NULL,
    CONSTRAINT chk_compuesto_factores CHECK (factor_tiempo > 0 AND factor_desgaste > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS estado_clima_dinamico (
    codigo VARCHAR(30) PRIMARY KEY,
    etiqueta VARCHAR(50) NOT NULL UNIQUE,
    condicion_codigo VARCHAR(20) NOT NULL,
    factor_tiempo_base DECIMAL(8,5) NOT NULL,
    estado_pista VARCHAR(60) NOT NULL,
    neumatico_recomendado VARCHAR(60) NOT NULL,
    estrategia_recomendada VARCHAR(80) NOT NULL,
    CONSTRAINT fk_estado_clima_condicion FOREIGN KEY (condicion_codigo)
        REFERENCES condicion_climatica (codigo),
    CONSTRAINT chk_estado_clima_factor CHECK (factor_tiempo_base > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS categoria_evento (
    codigo VARCHAR(30) PRIMARY KEY,
    etiqueta VARCHAR(60) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS alcance_evento (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(40) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS bandera_pista (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(60) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tipo_evento (
    codigo VARCHAR(40) PRIMARY KEY,
    etiqueta VARCHAR(100) NOT NULL UNIQUE,
    categoria_codigo VARCHAR(30) NOT NULL,
    alcance_codigo VARCHAR(20) NOT NULL,
    peso_base DECIMAL(8,4) NOT NULL,
    cooldown_vueltas SMALLINT UNSIGNED NOT NULL,
    CONSTRAINT fk_tipo_evento_categoria FOREIGN KEY (categoria_codigo)
        REFERENCES categoria_evento (codigo),
    CONSTRAINT fk_tipo_evento_alcance FOREIGN KEY (alcance_codigo)
        REFERENCES alcance_evento (codigo),
    CONSTRAINT chk_tipo_evento_peso CHECK (peso_base >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sector_pista (
    codigo VARCHAR(20) PRIMARY KEY,
    numero TINYINT UNSIGNED NULL,
    etiqueta VARCHAR(40) NOT NULL UNIQUE,
    CONSTRAINT uq_sector_numero UNIQUE (numero),
    CONSTRAINT chk_sector_numero CHECK (numero IS NULL OR numero BETWEEN 1 AND 3)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS estado_vuelta (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(50) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS fase_pit_stop (
    codigo VARCHAR(20) PRIMARY KEY,
    etiqueta VARCHAR(60) NOT NULL UNIQUE,
    completada BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS motivo_pit_stop (
    codigo VARCHAR(30) PRIMARY KEY,
    etiqueta VARCHAR(80) NOT NULL UNIQUE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Maestros procedentes del seed
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS equipo (
    equipo_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    pais_id SMALLINT UNSIGNED NOT NULL,
    fabricante_motor_id SMALLINT UNSIGNED NOT NULL,
    imagen_url VARCHAR(512) NULL,
    CONSTRAINT uq_equipo_nombre UNIQUE (nombre),
    CONSTRAINT fk_equipo_pais FOREIGN KEY (pais_id) REFERENCES pais (pais_id),
    CONSTRAINT fk_equipo_motor FOREIGN KEY (fabricante_motor_id)
        REFERENCES fabricante_motor (fabricante_motor_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS piloto (
    piloto_id SMALLINT UNSIGNED PRIMARY KEY,
    equipo_id SMALLINT UNSIGNED NOT NULL,
    rol_codigo VARCHAR(20) NOT NULL,
    nacionalidad_id SMALLINT UNSIGNED NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    experiencia_anios TINYINT UNSIGNED NOT NULL,
    numero SMALLINT UNSIGNED NOT NULL,
    codigo_tv CHAR(3) NOT NULL,
    victorias SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    campeonatos TINYINT UNSIGNED NOT NULL DEFAULT 0,
    imagen_url VARCHAR(512) NULL,
    CONSTRAINT uq_piloto_nombre UNIQUE (nombre),
    CONSTRAINT uq_piloto_numero UNIQUE (numero),
    CONSTRAINT uq_piloto_codigo_tv UNIQUE (codigo_tv),
    CONSTRAINT fk_piloto_equipo FOREIGN KEY (equipo_id) REFERENCES equipo (equipo_id),
    CONSTRAINT fk_piloto_rol FOREIGN KEY (rol_codigo) REFERENCES rol_piloto (codigo),
    CONSTRAINT fk_piloto_nacionalidad FOREIGN KEY (nacionalidad_id)
        REFERENCES nacionalidad (nacionalidad_id),
    CONSTRAINT chk_piloto_experiencia CHECK (experiencia_anios <= 50)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS piloto_habilidad (
    piloto_id SMALLINT UNSIGNED NOT NULL,
    habilidad_codigo VARCHAR(24) NOT NULL,
    valor TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (piloto_id, habilidad_codigo),
    CONSTRAINT fk_piloto_habilidad_piloto FOREIGN KEY (piloto_id)
        REFERENCES piloto (piloto_id) ON DELETE CASCADE,
    CONSTRAINT fk_piloto_habilidad_tipo FOREIGN KEY (habilidad_codigo)
        REFERENCES tipo_habilidad (codigo),
    CONSTRAINT chk_piloto_habilidad_valor CHECK (valor BETWEEN 0 AND 100)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS vehiculo (
    vehiculo_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    equipo_id SMALLINT UNSIGNED NOT NULL,
    fabricante_motor_id SMALLINT UNSIGNED NOT NULL,
    modelo VARCHAR(40) NOT NULL,
    velocidad_maxima_kmh SMALLINT UNSIGNED NOT NULL,
    aceleracion_0_100 DECIMAL(5,2) NOT NULL,
    imagen_url VARCHAR(512) NULL,
    CONSTRAINT uq_vehiculo_modelo UNIQUE (modelo),
    CONSTRAINT fk_vehiculo_equipo FOREIGN KEY (equipo_id) REFERENCES equipo (equipo_id),
    CONSTRAINT fk_vehiculo_motor FOREIGN KEY (fabricante_motor_id)
        REFERENCES fabricante_motor (fabricante_motor_id),
    CONSTRAINT chk_vehiculo_velocidad CHECK (velocidad_maxima_kmh BETWEEN 1 AND 500),
    CONSTRAINT chk_vehiculo_aceleracion CHECK (aceleracion_0_100 > 0)
) ENGINE = InnoDB;

-- Relacion independiente de las listas equipo.pilotos y vehiculo.pilotos del JSON.
CREATE TABLE IF NOT EXISTS vehiculo_piloto (
    vehiculo_id SMALLINT UNSIGNED NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (vehiculo_id, piloto_id),
    CONSTRAINT fk_vehiculo_piloto_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculo (vehiculo_id) ON DELETE CASCADE,
    CONSTRAINT fk_vehiculo_piloto_piloto FOREIGN KEY (piloto_id)
        REFERENCES piloto (piloto_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS vehiculo_rendimiento (
    vehiculo_id SMALLINT UNSIGNED NOT NULL,
    modo_codigo VARCHAR(32) NOT NULL,
    velocidad_promedio_kmh SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (vehiculo_id, modo_codigo),
    CONSTRAINT fk_rendimiento_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculo (vehiculo_id) ON DELETE CASCADE,
    CONSTRAINT fk_rendimiento_modo FOREIGN KEY (modo_codigo)
        REFERENCES modo_conduccion (codigo),
    CONSTRAINT chk_rendimiento_velocidad CHECK (velocidad_promedio_kmh BETWEEN 1 AND 500)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS vehiculo_rendimiento_clima (
    vehiculo_id SMALLINT UNSIGNED NOT NULL,
    modo_codigo VARCHAR(32) NOT NULL,
    condicion_codigo VARCHAR(20) NOT NULL,
    consumo_combustible DECIMAL(7,3) NOT NULL,
    desgaste_neumaticos DECIMAL(7,3) NOT NULL,
    PRIMARY KEY (vehiculo_id, modo_codigo, condicion_codigo),
    CONSTRAINT fk_rendimiento_clima_base FOREIGN KEY (vehiculo_id, modo_codigo)
        REFERENCES vehiculo_rendimiento (vehiculo_id, modo_codigo) ON DELETE CASCADE,
    CONSTRAINT fk_rendimiento_clima_condicion FOREIGN KEY (condicion_codigo)
        REFERENCES condicion_climatica (codigo),
    CONSTRAINT chk_rendimiento_clima_valores CHECK (
        consumo_combustible >= 0 AND desgaste_neumaticos >= 0
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS circuito (
    circuito_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pais_id SMALLINT UNSIGNED NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    longitud_km DECIMAL(7,3) NOT NULL,
    vueltas SMALLINT UNSIGNED NOT NULL,
    descripcion TEXT NULL,
    factor_tecnico DECIMAL(8,5) NOT NULL DEFAULT 1.40000,
    factor_consumo DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
    factor_desgaste DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
    imagen_url VARCHAR(512) NULL,
    CONSTRAINT uq_circuito_nombre UNIQUE (nombre),
    CONSTRAINT fk_circuito_pais FOREIGN KEY (pais_id) REFERENCES pais (pais_id),
    CONSTRAINT chk_circuito_longitud CHECK (longitud_km > 0),
    CONSTRAINT chk_circuito_vueltas CHECK (vueltas > 0),
    CONSTRAINT chk_circuito_factores CHECK (
        factor_tecnico > 0 AND factor_consumo > 0 AND factor_desgaste > 0
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS circuito_record_vuelta (
    circuito_id SMALLINT UNSIGNED PRIMARY KEY,
    tiempo_segundos DECIMAL(9,3) NOT NULL,
    titular_nombre VARCHAR(100) NOT NULL,
    anio SMALLINT UNSIGNED NOT NULL,
    CONSTRAINT fk_record_circuito FOREIGN KEY (circuito_id)
        REFERENCES circuito (circuito_id) ON DELETE CASCADE,
    CONSTRAINT chk_record_tiempo CHECK (tiempo_segundos > 0),
    CONSTRAINT chk_record_anio CHECK (anio BETWEEN 1950 AND 2200)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS circuito_probabilidad_clima (
    circuito_id SMALLINT UNSIGNED NOT NULL,
    condicion_codigo VARCHAR(20) NOT NULL,
    probabilidad DECIMAL(8,7) NOT NULL,
    PRIMARY KEY (circuito_id, condicion_codigo),
    CONSTRAINT fk_probabilidad_circuito FOREIGN KEY (circuito_id)
        REFERENCES circuito (circuito_id) ON DELETE CASCADE,
    CONSTRAINT fk_probabilidad_condicion FOREIGN KEY (condicion_codigo)
        REFERENCES condicion_climatica (codigo),
    CONSTRAINT chk_probabilidad_clima CHECK (probabilidad BETWEEN 0 AND 1)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS circuito_ganador (
    circuito_id SMALLINT UNSIGNED NOT NULL,
    temporada SMALLINT UNSIGNED NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (circuito_id, temporada),
    CONSTRAINT fk_ganador_circuito FOREIGN KEY (circuito_id)
        REFERENCES circuito (circuito_id) ON DELETE CASCADE,
    CONSTRAINT fk_ganador_piloto FOREIGN KEY (piloto_id)
        REFERENCES piloto (piloto_id),
    CONSTRAINT chk_ganador_temporada CHECK (temporada BETWEEN 1950 AND 2200)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Configuracion probabilistica de eventos
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS perfil_probabilidad_evento (
    perfil_id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    probabilidad_coexistencia_global DECIMAL(8,7) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_perfil_coexistencia CHECK (probabilidad_coexistencia_global BETWEEN 0 AND 1)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS perfil_probabilidad_categoria (
    perfil_id SMALLINT UNSIGNED NOT NULL,
    categoria_codigo VARCHAR(30) NOT NULL,
    probabilidad DECIMAL(8,7) NOT NULL,
    PRIMARY KEY (perfil_id, categoria_codigo),
    CONSTRAINT fk_perfil_categoria_perfil FOREIGN KEY (perfil_id)
        REFERENCES perfil_probabilidad_evento (perfil_id) ON DELETE CASCADE,
    CONSTRAINT fk_perfil_categoria_categoria FOREIGN KEY (categoria_codigo)
        REFERENCES categoria_evento (codigo),
    CONSTRAINT chk_perfil_categoria_probabilidad CHECK (probabilidad BETWEEN 0 AND 1)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Sesiones y estructuras antes embebidas en MongoDB
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS configuracion_simulacion (
    configuracion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    circuito_id SMALLINT UNSIGNED NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    vehiculo_id SMALLINT UNSIGNED NOT NULL,
    modo_codigo VARCHAR(32) NOT NULL,
    carga_codigo VARCHAR(20) NOT NULL,
    presion_codigo VARCHAR(20) NOT NULL,
    compuesto_inicial_codigo CHAR(1) NOT NULL,
    estrategia_combustible_codigo VARCHAR(20) NOT NULL,
    duracion_segundos SMALLINT UNSIGNED NOT NULL DEFAULT 10,
    circuito_nombre_snapshot VARCHAR(120) NOT NULL,
    piloto_nombre_snapshot VARCHAR(100) NOT NULL,
    vehiculo_modelo_snapshot VARCHAR(40) NOT NULL,
    guardado_en DATETIME(3) NULL,
    CONSTRAINT fk_config_circuito FOREIGN KEY (circuito_id) REFERENCES circuito (circuito_id),
    CONSTRAINT fk_config_vehiculo_piloto FOREIGN KEY (vehiculo_id, piloto_id)
        REFERENCES vehiculo_piloto (vehiculo_id, piloto_id),
    CONSTRAINT fk_config_modo FOREIGN KEY (modo_codigo) REFERENCES modo_conduccion (codigo),
    CONSTRAINT fk_config_carga FOREIGN KEY (carga_codigo) REFERENCES carga_aerodinamica (codigo),
    CONSTRAINT fk_config_presion FOREIGN KEY (presion_codigo) REFERENCES presion_neumatico (codigo),
    CONSTRAINT fk_config_compuesto FOREIGN KEY (compuesto_inicial_codigo)
        REFERENCES compuesto_neumatico (codigo),
    CONSTRAINT fk_config_estrategia FOREIGN KEY (estrategia_combustible_codigo)
        REFERENCES estrategia_combustible (codigo),
    CONSTRAINT chk_config_duracion CHECK (duracion_segundos BETWEEN 1 AND 3600)
) ENGINE = InnoDB;

-- Los campos *_snapshot son hechos históricos de la sesión, no copias del
-- estado maestro actual: preservan exactamente los nombres guardados por MongoDB.

CREATE TABLE IF NOT EXISTS sesion_clasificacion (
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    configuracion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    condicion_inicial_codigo VARCHAR(20) NOT NULL,
    total_segmentos INT UNSIGNED NOT NULL,
    fecha DATETIME(3) NOT NULL,
    CONSTRAINT uq_sesion_configuracion UNIQUE (configuracion_id),
    CONSTRAINT fk_sesion_configuracion FOREIGN KEY (configuracion_id)
        REFERENCES configuracion_simulacion (configuracion_id),
    CONSTRAINT fk_sesion_condicion FOREIGN KEY (condicion_inicial_codigo)
        REFERENCES condicion_climatica (codigo),
    CONSTRAINT chk_sesion_total_segmentos CHECK (total_segmentos > 0),
    INDEX idx_sesion_fecha (fecha)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS resultado_vuelta (
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    vehiculo_id SMALLINT UNSIGNED NOT NULL,
    posicion TINYINT UNSIGNED NOT NULL,
    tiempo_segundos DECIMAL(10,6) NOT NULL,
    gap_segundos DECIMAL(10,6) NOT NULL,
    consumo_estimado DECIMAL(9,5) NOT NULL,
    desgaste_estimado DECIMAL(9,5) NOT NULL,
    piloto_nombre_snapshot VARCHAR(100) NOT NULL,
    equipo_nombre_snapshot VARCHAR(100) NOT NULL,
    vehiculo_modelo_snapshot VARCHAR(40) NOT NULL,
    estado_codigo VARCHAR(20) NOT NULL,
    sector_incidente_codigo VARCHAR(20) NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (sesion_id, piloto_id),
    CONSTRAINT uq_resultado_posicion UNIQUE (sesion_id, posicion),
    CONSTRAINT fk_resultado_sesion FOREIGN KEY (sesion_id)
        REFERENCES sesion_clasificacion (sesion_id) ON DELETE CASCADE,
    CONSTRAINT fk_resultado_vehiculo_piloto FOREIGN KEY (vehiculo_id, piloto_id)
        REFERENCES vehiculo_piloto (vehiculo_id, piloto_id),
    CONSTRAINT fk_resultado_estado FOREIGN KEY (estado_codigo) REFERENCES estado_vuelta (codigo),
    CONSTRAINT fk_resultado_sector FOREIGN KEY (sector_incidente_codigo)
        REFERENCES sector_pista (codigo),
    CONSTRAINT chk_resultado_posicion CHECK (posicion > 0),
    CONSTRAINT chk_resultado_metricas CHECK (
        tiempo_segundos >= 0 AND gap_segundos >= 0
        AND consumo_estimado >= 0 AND desgaste_estimado >= 0
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tiempo_sector (
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    sector_codigo VARCHAR(20) NOT NULL,
    tiempo_segundos DECIMAL(10,6) NOT NULL,
    PRIMARY KEY (sesion_id, piloto_id, sector_codigo),
    CONSTRAINT fk_tiempo_sector_resultado FOREIGN KEY (sesion_id, piloto_id)
        REFERENCES resultado_vuelta (sesion_id, piloto_id) ON DELETE CASCADE,
    CONSTRAINT fk_tiempo_sector_sector FOREIGN KEY (sector_codigo)
        REFERENCES sector_pista (codigo),
    CONSTRAINT chk_tiempo_sector_positivo CHECK (tiempo_segundos > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS clima_snapshot (
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    segmento INT UNSIGNED NOT NULL,
    estado_clima_codigo VARCHAR(30) NOT NULL,
    temperatura_c DECIMAL(7,3) NOT NULL,
    humedad_porcentaje DECIMAL(7,3) NOT NULL,
    probabilidad_lluvia_porcentaje DECIMAL(7,3) NOT NULL,
    intensidad_lluvia_porcentaje DECIMAL(7,3) NOT NULL,
    temperatura_pista_c DECIMAL(7,3) NOT NULL,
    grip_porcentaje DECIMAL(7,3) NOT NULL,
    traccion_porcentaje DECIMAL(7,3) NOT NULL,
    frenado_porcentaje DECIMAL(7,3) NOT NULL,
    PRIMARY KEY (sesion_id, segmento),
    CONSTRAINT fk_clima_snapshot_sesion FOREIGN KEY (sesion_id)
        REFERENCES sesion_clasificacion (sesion_id) ON DELETE CASCADE,
    CONSTRAINT fk_clima_snapshot_estado FOREIGN KEY (estado_clima_codigo)
        REFERENCES estado_clima_dinamico (codigo),
    CONSTRAINT chk_clima_snapshot_segmento CHECK (segmento > 0),
    CONSTRAINT chk_clima_snapshot_temperaturas CHECK (
        temperatura_c BETWEEN -20 AND 60
        AND temperatura_pista_c BETWEEN -20 AND 80
    ),
    CONSTRAINT chk_clima_snapshot_porcentajes CHECK (
        humedad_porcentaje BETWEEN 0 AND 100
        AND probabilidad_lluvia_porcentaje BETWEEN 0 AND 100
        AND intensidad_lluvia_porcentaje BETWEEN 0 AND 100
        AND grip_porcentaje BETWEEN 0 AND 100
        AND traccion_porcentaje BETWEEN 0 AND 100
        AND frenado_porcentaje BETWEEN 0 AND 100
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS evento_sesion (
    evento_sesion_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tipo_evento_codigo VARCHAR(40) NOT NULL,
    piloto_id SMALLINT UNSIGNED NULL,
    piloto_nombre_snapshot VARCHAR(100) NULL,
    numero_vuelta SMALLINT UNSIGNED NOT NULL,
    sector_codigo VARCHAR(20) NOT NULL,
    delta_tiempo_segundos DECIMAL(9,6) NOT NULL DEFAULT 0,
    multiplicador_velocidad DECIMAL(8,5) NOT NULL DEFAULT 1,
    delta_grip_porcentaje DECIMAL(8,4) NOT NULL DEFAULT 0,
    delta_desgaste DECIMAL(8,4) NOT NULL DEFAULT 0,
    delta_temperatura_neumaticos_c DECIMAL(8,4) NOT NULL DEFAULT 0,
    delta_temperatura_motor_c DECIMAL(8,4) NOT NULL DEFAULT 0,
    delta_intensidad_lluvia_porcentaje DECIMAL(8,4) NOT NULL DEFAULT 0,
    vuelta_invalidada BOOLEAN NOT NULL DEFAULT FALSE,
    piloto_fuera BOOLEAN NOT NULL DEFAULT FALSE,
    bandera_codigo VARCHAR(20) NOT NULL DEFAULT 'GREEN',
    CONSTRAINT fk_evento_sesion FOREIGN KEY (sesion_id)
        REFERENCES sesion_clasificacion (sesion_id) ON DELETE CASCADE,
    CONSTRAINT fk_evento_tipo FOREIGN KEY (tipo_evento_codigo) REFERENCES tipo_evento (codigo),
    CONSTRAINT fk_evento_piloto FOREIGN KEY (piloto_id) REFERENCES piloto (piloto_id),
    CONSTRAINT fk_evento_sector FOREIGN KEY (sector_codigo) REFERENCES sector_pista (codigo),
    CONSTRAINT fk_evento_bandera FOREIGN KEY (bandera_codigo) REFERENCES bandera_pista (codigo),
    CONSTRAINT chk_evento_vuelta CHECK (numero_vuelta > 0),
    CONSTRAINT chk_evento_velocidad CHECK (multiplicador_velocidad BETWEEN 0 AND 1.2),
    CONSTRAINT chk_evento_impactos CHECK (
        delta_grip_porcentaje BETWEEN -40 AND 20
        AND delta_desgaste BETWEEN -20 AND 40
        AND delta_temperatura_neumaticos_c BETWEEN -40 AND 40
        AND delta_temperatura_motor_c BETWEEN -30 AND 40
        AND delta_intensidad_lluvia_porcentaje BETWEEN -100 AND 100
    ),
    CONSTRAINT chk_evento_fuera CHECK (piloto_fuera = FALSE OR vuelta_invalidada = TRUE),
    INDEX idx_evento_sesion_piloto (sesion_id, piloto_id),
    INDEX idx_evento_sesion_sector (sesion_id, sector_codigo)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS telemetria_snapshot (
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    segmento INT UNSIGNED NOT NULL,
    velocidad_kmh DECIMAL(8,3) NOT NULL,
    velocidad_maxima_kmh DECIMAL(8,3) NOT NULL,
    rpm SMALLINT UNSIGNED NOT NULL,
    combustible_restante_porcentaje DECIMAL(7,3) NOT NULL,
    desgaste_neumaticos_porcentaje DECIMAL(7,3) NOT NULL,
    temperatura_neumaticos_c DECIMAL(7,3) NOT NULL,
    temperatura_motor_c DECIMAL(7,3) NOT NULL,
    sector_codigo VARCHAR(20) NOT NULL,
    tiempo_vuelta_segundos DECIMAL(10,6) NOT NULL,
    delta_segundos DECIMAL(10,6) NOT NULL,
    estado_vuelta_codigo VARCHAR(20) NOT NULL,
    evento_sesion_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (sesion_id, segmento),
    CONSTRAINT fk_telemetria_clima FOREIGN KEY (sesion_id, segmento)
        REFERENCES clima_snapshot (sesion_id, segmento) ON DELETE CASCADE,
    CONSTRAINT fk_telemetria_sector FOREIGN KEY (sector_codigo) REFERENCES sector_pista (codigo),
    CONSTRAINT fk_telemetria_estado FOREIGN KEY (estado_vuelta_codigo)
        REFERENCES estado_vuelta (codigo),
    CONSTRAINT fk_telemetria_evento FOREIGN KEY (evento_sesion_id)
        REFERENCES evento_sesion (evento_sesion_id) ON DELETE SET NULL,
    CONSTRAINT chk_telemetria_segmento CHECK (segmento > 0),
    CONSTRAINT chk_telemetria_rpm CHECK (rpm <= 20000),
    CONSTRAINT chk_telemetria_velocidad CHECK (
        velocidad_maxima_kmh BETWEEN 1 AND 500
        AND velocidad_kmh BETWEEN 0 AND velocidad_maxima_kmh
    ),
    CONSTRAINT chk_telemetria_temperaturas CHECK (
        temperatura_neumaticos_c BETWEEN 0 AND 150
        AND temperatura_motor_c BETWEEN 0 AND 160
        AND tiempo_vuelta_segundos BETWEEN 0 AND 600
    ),
    CONSTRAINT chk_telemetria_porcentajes CHECK (
        combustible_restante_porcentaje BETWEEN 0 AND 100
        AND desgaste_neumaticos_porcentaje BETWEEN 0 AND 100
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS evolucion_pista (
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    numero_vuelta SMALLINT UNSIGNED NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    piloto_nombre_snapshot VARCHAR(100) NOT NULL,
    grip_inicial_porcentaje DECIMAL(7,3) NOT NULL,
    grip_final_porcentaje DECIMAL(7,3) NOT NULL,
    goma_inicial_porcentaje DECIMAL(7,3) NOT NULL,
    goma_final_porcentaje DECIMAL(7,3) NOT NULL,
    lluvia_promedio_porcentaje DECIMAL(7,3) NOT NULL,
    PRIMARY KEY (sesion_id, numero_vuelta),
    CONSTRAINT uq_evolucion_pista_piloto UNIQUE (sesion_id, piloto_id),
    CONSTRAINT fk_evolucion_pista_sesion FOREIGN KEY (sesion_id)
        REFERENCES sesion_clasificacion (sesion_id) ON DELETE CASCADE,
    CONSTRAINT fk_evolucion_pista_piloto FOREIGN KEY (piloto_id) REFERENCES piloto (piloto_id),
    CONSTRAINT chk_evolucion_pista_vuelta CHECK (numero_vuelta > 0),
    CONSTRAINT chk_evolucion_pista_porcentajes CHECK (
        grip_inicial_porcentaje BETWEEN 0 AND 100
        AND grip_final_porcentaje BETWEEN 0 AND 100
        AND goma_inicial_porcentaje BETWEEN 0 AND 100
        AND goma_final_porcentaje BETWEEN 0 AND 100
        AND lluvia_promedio_porcentaje BETWEEN 0 AND 100
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pit_stop (
    pit_stop_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    sesion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    piloto_id SMALLINT UNSIGNED NOT NULL,
    piloto_nombre_snapshot VARCHAR(100) NOT NULL,
    numero_vuelta SMALLINT UNSIGNED NOT NULL,
    segmento_entrada INT UNSIGNED NOT NULL,
    segmento_actual INT UNSIGNED NOT NULL,
    fase_codigo VARCHAR(20) NOT NULL,
    motivo_codigo VARCHAR(30) NOT NULL,
    tiempo_detenido_segundos DECIMAL(9,6) NOT NULL,
    tiempo_perdido_segundos DECIMAL(9,6) NOT NULL,
    posicion_entrada TINYINT UNSIGNED NOT NULL,
    posicion_actual TINYINT UNSIGNED NOT NULL,
    CONSTRAINT uq_pit_stop_sesion_piloto UNIQUE (sesion_id, piloto_id),
    CONSTRAINT fk_pit_stop_resultado FOREIGN KEY (sesion_id, piloto_id)
        REFERENCES resultado_vuelta (sesion_id, piloto_id) ON DELETE CASCADE,
    CONSTRAINT fk_pit_stop_fase FOREIGN KEY (fase_codigo) REFERENCES fase_pit_stop (codigo),
    CONSTRAINT fk_pit_stop_motivo FOREIGN KEY (motivo_codigo) REFERENCES motivo_pit_stop (codigo),
    CONSTRAINT chk_pit_stop_segmentos CHECK (
        numero_vuelta > 0 AND segmento_entrada > 0 AND segmento_actual >= segmento_entrada
    ),
    CONSTRAINT chk_pit_stop_tiempos CHECK (
        tiempo_detenido_segundos >= 0 AND tiempo_perdido_segundos >= 0
    ),
    CONSTRAINT chk_pit_stop_posiciones CHECK (posicion_entrada > 0 AND posicion_actual > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS cambio_neumatico (
    pit_stop_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    segmento INT UNSIGNED NOT NULL,
    compuesto_anterior_codigo CHAR(1) NOT NULL,
    compuesto_nuevo_codigo CHAR(1) NOT NULL,
    CONSTRAINT fk_cambio_neumatico_pit FOREIGN KEY (pit_stop_id)
        REFERENCES pit_stop (pit_stop_id) ON DELETE CASCADE,
    CONSTRAINT fk_cambio_compuesto_anterior FOREIGN KEY (compuesto_anterior_codigo)
        REFERENCES compuesto_neumatico (codigo),
    CONSTRAINT fk_cambio_compuesto_nuevo FOREIGN KEY (compuesto_nuevo_codigo)
        REFERENCES compuesto_neumatico (codigo),
    CONSTRAINT chk_cambio_segmento CHECK (segmento > 0),
    CONSTRAINT chk_cambio_compuesto CHECK (compuesto_anterior_codigo <> compuesto_nuevo_codigo)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Reglas entre tablas y vistas de control
-- ---------------------------------------------------------------------------

DELIMITER $$

DROP TRIGGER IF EXISTS trg_vehiculo_piloto_equipo_insert$$
CREATE TRIGGER trg_vehiculo_piloto_equipo_insert
BEFORE INSERT ON vehiculo_piloto
FOR EACH ROW
BEGIN
    DECLARE v_equipo_vehiculo SMALLINT UNSIGNED;
    DECLARE v_equipo_piloto SMALLINT UNSIGNED;
    SELECT equipo_id INTO v_equipo_vehiculo
      FROM vehiculo WHERE vehiculo_id = NEW.vehiculo_id;
    SELECT equipo_id INTO v_equipo_piloto
      FROM piloto WHERE piloto_id = NEW.piloto_id;
    IF v_equipo_vehiculo <> v_equipo_piloto THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El piloto y el vehiculo deben pertenecer al mismo equipo';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_vehiculo_piloto_equipo_update$$
CREATE TRIGGER trg_vehiculo_piloto_equipo_update
BEFORE UPDATE ON vehiculo_piloto
FOR EACH ROW
BEGIN
    DECLARE v_equipo_vehiculo SMALLINT UNSIGNED;
    DECLARE v_equipo_piloto SMALLINT UNSIGNED;
    SELECT equipo_id INTO v_equipo_vehiculo
      FROM vehiculo WHERE vehiculo_id = NEW.vehiculo_id;
    SELECT equipo_id INTO v_equipo_piloto
      FROM piloto WHERE piloto_id = NEW.piloto_id;
    IF v_equipo_vehiculo <> v_equipo_piloto THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El piloto y el vehiculo deben pertenecer al mismo equipo';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_clima_segmento_insert$$
CREATE TRIGGER trg_clima_segmento_insert
BEFORE INSERT ON clima_snapshot
FOR EACH ROW
BEGIN
    DECLARE v_total_segmentos INT UNSIGNED;
    SELECT total_segmentos INTO v_total_segmentos
      FROM sesion_clasificacion WHERE sesion_id = NEW.sesion_id;
    IF NEW.segmento > v_total_segmentos THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El segmento climático excede el total de la sesión';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_clima_segmento_update$$
CREATE TRIGGER trg_clima_segmento_update
BEFORE UPDATE ON clima_snapshot
FOR EACH ROW
BEGIN
    DECLARE v_total_segmentos INT UNSIGNED;
    SELECT total_segmentos INTO v_total_segmentos
      FROM sesion_clasificacion WHERE sesion_id = NEW.sesion_id;
    IF NEW.segmento > v_total_segmentos THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El segmento climático excede el total de la sesión';
    END IF;
END$$

DELIMITER ;

CREATE OR REPLACE VIEW vw_probabilidad_clima_invalida AS
SELECT c.circuito_id,
       c.nombre,
       COUNT(pc.condicion_codigo) AS condiciones,
       COALESCE(SUM(pc.probabilidad), 0) AS probabilidad_total
FROM circuito AS c
LEFT JOIN circuito_probabilidad_clima AS pc ON pc.circuito_id = c.circuito_id
GROUP BY c.circuito_id, c.nombre
HAVING condiciones <> 3 OR ABS(probabilidad_total - 1.0) > 0.0000001;

CREATE OR REPLACE VIEW vw_perfil_evento_invalido AS
SELECT p.perfil_id,
       p.nombre,
       COUNT(pc.categoria_codigo) AS categorias,
       COALESCE(SUM(pc.probabilidad), 0) AS probabilidad_total
FROM perfil_probabilidad_evento AS p
LEFT JOIN perfil_probabilidad_categoria AS pc ON pc.perfil_id = p.perfil_id
GROUP BY p.perfil_id, p.nombre
HAVING categorias <> 6 OR ABS(probabilidad_total - 1.0) > 0.0000001;

CREATE OR REPLACE VIEW vw_clasificacion_detalle AS
SELECT r.sesion_id,
       s.fecha,
       cfg.circuito_nombre_snapshot AS circuito,
       r.posicion,
       r.piloto_nombre_snapshot AS piloto,
       p.codigo_tv,
       r.equipo_nombre_snapshot AS equipo,
       r.vehiculo_modelo_snapshot AS vehiculo,
       r.tiempo_segundos,
       r.gap_segundos,
       r.estado_codigo
FROM resultado_vuelta AS r
JOIN sesion_clasificacion AS s ON s.sesion_id = r.sesion_id
JOIN configuracion_simulacion AS cfg ON cfg.configuracion_id = s.configuracion_id
JOIN piloto AS p ON p.piloto_id = r.piloto_id;

