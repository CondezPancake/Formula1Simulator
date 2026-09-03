-- Formula1Simulator - verificación final para DBeaver/MySQL
-- Este archivo solo consulta información; no modifica datos.

USE formula1_simulator;

-- 1. Resumen de objetos creados.
SELECT table_type, COUNT(*) AS cantidad
FROM information_schema.tables
WHERE table_schema = 'formula1_simulator'
GROUP BY table_type
ORDER BY table_type;

SELECT COUNT(*) AS triggers_creados
FROM information_schema.triggers
WHERE trigger_schema = 'formula1_simulator';

SELECT COUNT(*) AS procedimientos_creados
FROM information_schema.routines
WHERE routine_schema = 'formula1_simulator'
  AND routine_type = 'PROCEDURE';

-- 2. Conteos principales de la carga inicial.
SELECT
    (SELECT COUNT(*) FROM equipo) AS equipos,
    (SELECT COUNT(*) FROM piloto) AS pilotos,
    (SELECT COUNT(*) FROM vehiculo) AS vehiculos,
    (SELECT COUNT(*) FROM circuito) AS circuitos,
    (SELECT COUNT(*) FROM vehiculo_piloto) AS asignaciones_vehiculo_piloto;

-- 3. Estas dos vistas deben devolver cero filas.
SELECT * FROM vw_probabilidad_clima_invalida;
SELECT * FROM vw_perfil_evento_invalido;

-- 4. Verifica que cada piloto esté vinculado a un vehículo de su mismo equipo.
-- Debe devolver cero filas.
SELECT vp.vehiculo_id,
       vp.piloto_id,
       v.equipo_id AS equipo_vehiculo,
       p.equipo_id AS equipo_piloto
FROM vehiculo_piloto AS vp
JOIN vehiculo AS v ON v.vehiculo_id = vp.vehiculo_id
JOIN piloto AS p ON p.piloto_id = vp.piloto_id
WHERE v.equipo_id <> p.equipo_id;

-- 5. Resultado general esperado: OK.
SELECT CASE
           WHEN (SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = 'formula1_simulator'
                   AND table_type = 'BASE TABLE') = 43
            AND (SELECT COUNT(*) FROM information_schema.views
                 WHERE table_schema = 'formula1_simulator') = 3
            AND (SELECT COUNT(*) FROM information_schema.triggers
                 WHERE trigger_schema = 'formula1_simulator') = 4
            AND (SELECT COUNT(*) FROM information_schema.routines
                 WHERE routine_schema = 'formula1_simulator'
                   AND routine_type = 'PROCEDURE') = 4
            AND (SELECT COUNT(*) FROM piloto) >= 20
            AND (SELECT COUNT(*) FROM equipo) >= 10
            AND (SELECT COUNT(*) FROM vehiculo) >= 10
            AND (SELECT COUNT(*) FROM circuito) >= 7
            AND NOT EXISTS (SELECT 1 FROM vw_probabilidad_clima_invalida)
            AND NOT EXISTS (SELECT 1 FROM vw_perfil_evento_invalido)
           THEN 'OK'
           ELSE 'REVISAR RESULTADOS ANTERIORES'
       END AS estado_general;
