-- Formula1Simulator - MySQL 8.0.19+
-- DCL: roles con mínimo privilegio. Ejecutar con una cuenta administradora.

USE formula1_simulator;

CREATE ROLE IF NOT EXISTS
    'f1_lectura',
    'f1_simulador',
    'f1_administrador';

-- Consultas, dashboard, resultados, telemetría e historial.
GRANT SELECT ON formula1_simulator.* TO 'f1_lectura';

-- El motor puede consultar catálogos y mantener únicamente datos de simulación.
GRANT SELECT ON formula1_simulator.* TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.configuracion_simulacion TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.sesion_clasificacion TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.resultado_vuelta TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.tiempo_sector TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.clima_snapshot TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.evento_sesion TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.telemetria_snapshot TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.evolucion_pista TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.pit_stop TO 'f1_simulador';
GRANT INSERT, UPDATE, DELETE ON formula1_simulator.cambio_neumatico TO 'f1_simulador';
GRANT EXECUTE ON formula1_simulator.* TO 'f1_simulador';

-- Administración funcional del esquema, sin privilegios globales del servidor.
GRANT ALL PRIVILEGES ON formula1_simulator.* TO 'f1_administrador';

-- Plantilla segura: define la contraseña fuera del repositorio y descomenta.
-- CREATE USER IF NOT EXISTS 'formula1_app'@'localhost'
--     IDENTIFIED BY '<CONTRASENA_GESTIONADA_EXTERNAMENTE>';
-- GRANT 'f1_simulador' TO 'formula1_app'@'localhost';
-- SET DEFAULT ROLE 'f1_simulador' TO 'formula1_app'@'localhost';

-- Plantilla para un usuario de reportes de solo lectura.
-- CREATE USER IF NOT EXISTS 'formula1_reportes'@'localhost'
--     IDENTIFIED BY '<CONTRASENA_GESTIONADA_EXTERNAMENTE>';
-- GRANT 'f1_lectura' TO 'formula1_reportes'@'localhost';
-- SET DEFAULT ROLE 'f1_lectura' TO 'formula1_reportes'@'localhost';

-- Revocación de ejemplo para retirar acceso sin eliminar la cuenta:
-- REVOKE 'f1_simulador' FROM 'formula1_app'@'localhost';
