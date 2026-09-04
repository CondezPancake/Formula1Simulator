-- Formula1Simulator - comprobación previa para DBeaver/MySQL
-- Este archivo solo consulta información; no crea ni modifica datos.

SELECT VERSION() AS version_mysql,
       CURRENT_USER() AS usuario_autenticado,
       @@hostname AS servidor,
       @@port AS puerto,
       @@autocommit AS autocommit;

SELECT CASE
           WHEN VERSION() NOT LIKE '%MariaDB%'
            AND (
                CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) > 8
                OR (
                    CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) = 8
                    AND (
                        CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) > 0
                        OR CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 3), '.', -1) AS UNSIGNED) >= 19
                    )
                )
            ) THEN 'OK: versión compatible'
           ELSE 'REVISAR: se requiere MySQL 8.0.19 o superior'
       END AS compatibilidad;
