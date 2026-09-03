# Capa de datos MySQL

## Arquitectura

`DataStore` conserva los `ConcurrentHashMap` requeridos como estado temporal de
la aplicación. La persistencia duradera reside en MySQL y queda detrás de
`PersistencePort`, por lo que controladores, servicios y dominio no dependen de
JDBC.

El adaptador `MySqlPersistenceAdapter` coordina dos responsabilidades:

- `MySqlCatalogRepository` reconstruye y persiste equipos, pilotos, vehículos,
  circuitos y todas sus relaciones normalizadas.
- `MySqlSessionRepository` guarda y recupera en una sola transacción la
  configuración, parrilla, sectores, clima, eventos, telemetría, evolución de
  pista, paradas y cambios de neumático.

`DatabaseConnection` centraliza la configuración. Admite `DB_URL`, `DB_USER` y
`DB_PASSWORD`; los valores locales predeterminados apuntan a
`jdbc:mysql://localhost:3307/formula1_simulator`.

## Modelo físico

Los entregables consolidados son:

- `database/SQL/schema.sql`: DDL, restricciones, triggers y vistas.
- `database/SQL/data.sql`: catálogos y datos iniciales idempotentes.

La carpeta `database/mysql_conectar facil/` contiene la misma instalación
separada por pasos, junto con procedimientos, roles y verificaciones.

No se usan columnas JSON para ocultar relaciones. Los mapas anidados del modelo
Java se traducen a `vehiculo_rendimiento` y
`vehiculo_rendimiento_clima`; las colecciones de una sesión se distribuyen en
tablas hijas con claves foráneas.

## Tolerancia a fallos y pruebas

Si MySQL no está accesible, la interfaz puede arrancar temporalmente desde
`seed.json`; las pruebas unitarias usan explícitamente `DataStore.enMemoria()`.
La prueba `MySqlPersistenceAdapterTest` valida la carga de los cuatro catálogos
y el round-trip transaccional de una simulación completa.
