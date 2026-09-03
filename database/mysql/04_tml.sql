-- Formula1Simulator - MySQL 8.0.19+
-- TML/TCL: límites transaccionales para guardar unidades completas del dominio.

USE formula1_simulator;
SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_crear_sesion_clasificacion$$
CREATE PROCEDURE sp_crear_sesion_clasificacion(
    IN p_sesion_id CHAR(36),
    IN p_configuracion_id CHAR(36),
    IN p_circuito_id SMALLINT UNSIGNED,
    IN p_piloto_id SMALLINT UNSIGNED,
    IN p_vehiculo_id SMALLINT UNSIGNED,
    IN p_modo_codigo VARCHAR(32),
    IN p_carga_codigo VARCHAR(20),
    IN p_presion_codigo VARCHAR(20),
    IN p_compuesto_inicial_codigo CHAR(1),
    IN p_estrategia_combustible_codigo VARCHAR(20),
    IN p_duracion_segundos SMALLINT UNSIGNED,
    IN p_condicion_inicial_codigo VARCHAR(20),
    IN p_total_segmentos INT UNSIGNED,
    IN p_fecha DATETIME(3)
)
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    INSERT INTO configuracion_simulacion (
        configuracion_id,
        circuito_id,
        piloto_id,
        vehiculo_id,
        modo_codigo,
        carga_codigo,
        presion_codigo,
        compuesto_inicial_codigo,
        estrategia_combustible_codigo,
        duracion_segundos,
        circuito_nombre_snapshot,
        piloto_nombre_snapshot,
        vehiculo_modelo_snapshot,
        guardado_en
    )
    SELECT
        p_configuracion_id,
        p_circuito_id,
        p_piloto_id,
        p_vehiculo_id,
        p_modo_codigo,
        p_carga_codigo,
        p_presion_codigo,
        p_compuesto_inicial_codigo,
        p_estrategia_combustible_codigo,
        p_duracion_segundos,
        c.nombre,
        p.nombre,
        v.modelo,
        p_fecha
    FROM circuito AS c
    JOIN piloto AS p ON p.piloto_id = p_piloto_id
    JOIN vehiculo AS v ON v.vehiculo_id = p_vehiculo_id
    WHERE c.circuito_id = p_circuito_id;

    SAVEPOINT configuracion_creada;

    INSERT INTO sesion_clasificacion (
        sesion_id,
        configuracion_id,
        condicion_inicial_codigo,
        total_segmentos,
        fecha
    ) VALUES (
        p_sesion_id,
        p_configuracion_id,
        p_condicion_inicial_codigo,
        p_total_segmentos,
        p_fecha
    );

    COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_registrar_resultado_vuelta$$
CREATE PROCEDURE sp_registrar_resultado_vuelta(
    IN p_sesion_id CHAR(36),
    IN p_piloto_id SMALLINT UNSIGNED,
    IN p_vehiculo_id SMALLINT UNSIGNED,
    IN p_posicion TINYINT UNSIGNED,
    IN p_tiempo_segundos DECIMAL(10,6),
    IN p_gap_segundos DECIMAL(10,6),
    IN p_consumo_estimado DECIMAL(9,5),
    IN p_desgaste_estimado DECIMAL(9,5),
    IN p_estado_codigo VARCHAR(20),
    IN p_sector_incidente_codigo VARCHAR(20),
    IN p_sector_1_segundos DECIMAL(10,6),
    IN p_sector_2_segundos DECIMAL(10,6),
    IN p_sector_3_segundos DECIMAL(10,6)
)
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    INSERT INTO resultado_vuelta (
        sesion_id,
        piloto_id,
        vehiculo_id,
        posicion,
        tiempo_segundos,
        gap_segundos,
        consumo_estimado,
        desgaste_estimado,
        piloto_nombre_snapshot,
        equipo_nombre_snapshot,
        vehiculo_modelo_snapshot,
        estado_codigo,
        sector_incidente_codigo
    )
    SELECT
        p_sesion_id,
        p_piloto_id,
        p_vehiculo_id,
        p_posicion,
        p_tiempo_segundos,
        p_gap_segundos,
        p_consumo_estimado,
        p_desgaste_estimado,
        p.nombre,
        e.nombre,
        v.modelo,
        p_estado_codigo,
        p_sector_incidente_codigo
    FROM piloto AS p
    JOIN equipo AS e ON e.equipo_id = p.equipo_id
    JOIN vehiculo AS v ON v.vehiculo_id = p_vehiculo_id
    WHERE p.piloto_id = p_piloto_id;

    SAVEPOINT resultado_creado;

    INSERT INTO tiempo_sector (
        sesion_id,
        piloto_id,
        sector_codigo,
        tiempo_segundos
    ) VALUES
        (p_sesion_id, p_piloto_id, 'SECTOR_1', p_sector_1_segundos),
        (p_sesion_id, p_piloto_id, 'SECTOR_2', p_sector_2_segundos),
        (p_sesion_id, p_piloto_id, 'SECTOR_3', p_sector_3_segundos);

    COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_registrar_muestra_telemetria$$
CREATE PROCEDURE sp_registrar_muestra_telemetria(
    IN p_sesion_id CHAR(36),
    IN p_segmento INT UNSIGNED,
    IN p_estado_clima_codigo VARCHAR(30),
    IN p_temperatura_c DECIMAL(7,3),
    IN p_humedad_porcentaje DECIMAL(7,3),
    IN p_probabilidad_lluvia_porcentaje DECIMAL(7,3),
    IN p_intensidad_lluvia_porcentaje DECIMAL(7,3),
    IN p_temperatura_pista_c DECIMAL(7,3),
    IN p_grip_porcentaje DECIMAL(7,3),
    IN p_traccion_porcentaje DECIMAL(7,3),
    IN p_frenado_porcentaje DECIMAL(7,3),
    IN p_velocidad_kmh DECIMAL(8,3),
    IN p_velocidad_maxima_kmh DECIMAL(8,3),
    IN p_rpm SMALLINT UNSIGNED,
    IN p_combustible_restante_porcentaje DECIMAL(7,3),
    IN p_desgaste_neumaticos_porcentaje DECIMAL(7,3),
    IN p_temperatura_neumaticos_c DECIMAL(7,3),
    IN p_temperatura_motor_c DECIMAL(7,3),
    IN p_sector_codigo VARCHAR(20),
    IN p_tiempo_vuelta_segundos DECIMAL(10,6),
    IN p_delta_segundos DECIMAL(10,6),
    IN p_estado_vuelta_codigo VARCHAR(20),
    IN p_evento_sesion_id BIGINT UNSIGNED
)
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    INSERT INTO clima_snapshot (
        sesion_id, segmento, estado_clima_codigo,
        temperatura_c, humedad_porcentaje, probabilidad_lluvia_porcentaje,
        intensidad_lluvia_porcentaje, temperatura_pista_c, grip_porcentaje,
        traccion_porcentaje, frenado_porcentaje
    ) VALUES (
        p_sesion_id, p_segmento, p_estado_clima_codigo,
        p_temperatura_c, p_humedad_porcentaje, p_probabilidad_lluvia_porcentaje,
        p_intensidad_lluvia_porcentaje, p_temperatura_pista_c, p_grip_porcentaje,
        p_traccion_porcentaje, p_frenado_porcentaje
    );

    SAVEPOINT clima_registrado;

    INSERT INTO telemetria_snapshot (
        sesion_id, segmento,
        velocidad_kmh, velocidad_maxima_kmh, rpm,
        combustible_restante_porcentaje, desgaste_neumaticos_porcentaje,
        temperatura_neumaticos_c, temperatura_motor_c, sector_codigo,
        tiempo_vuelta_segundos, delta_segundos, estado_vuelta_codigo,
        evento_sesion_id
    ) VALUES (
        p_sesion_id, p_segmento,
        p_velocidad_kmh, p_velocidad_maxima_kmh, p_rpm,
        p_combustible_restante_porcentaje, p_desgaste_neumaticos_porcentaje,
        p_temperatura_neumaticos_c, p_temperatura_motor_c, p_sector_codigo,
        p_tiempo_vuelta_segundos, p_delta_segundos, p_estado_vuelta_codigo,
        p_evento_sesion_id
    );

    COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_sesion_clasificacion$$
CREATE PROCEDURE sp_eliminar_sesion_clasificacion(
    IN p_sesion_id CHAR(36)
)
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    DECLARE v_configuracion_id CHAR(36);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT configuracion_id
      INTO v_configuracion_id
      FROM sesion_clasificacion
     WHERE sesion_id = p_sesion_id
       FOR UPDATE;

    DELETE FROM sesion_clasificacion
     WHERE sesion_id = p_sesion_id;

    SAVEPOINT sesion_eliminada;

    DELETE FROM configuracion_simulacion
     WHERE configuracion_id = v_configuracion_id;

    COMMIT;
END$$

DELIMITER ;

-- Ejemplo de control manual cuando se migren documentos históricos por lotes:
-- START TRANSACTION;
-- SAVEPOINT lote_iniciado;
-- Ejecuta aquí las inserciones del lote histórico.
-- ROLLBACK TO SAVEPOINT lote_iniciado; -- ante una validación fallida del lote
-- COMMIT;                              -- solo cuando el lote completo sea válido
