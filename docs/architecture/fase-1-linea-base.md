# Fase 1 — línea base ejecutable

Registrado el 2026-09-03 en la rama `refactor/hexagonal-architecture` (creada
desde `feature/conde`), antes de tocar producción.

## `mvn clean test`

```
Tests run: 171, Failures: 0, Errors: 0, Skipped: 4
BUILD SUCCESS
```

- Los 4 *skipped* son los 4 tests de `MySqlPersistenceAdapterTest`, guardados
  tras `assumeTrue(DatabaseConnection.isAvailable())`. En esta máquina no hay
  un MySQL escuchando en `jdbc:mysql://localhost:3307` (verificado con
  `Test-NetConnection` y sin servicio/proceso `mysqld` local), así que quedan
  sin ejecutar aquí — no son un fallo, pero tampoco cobertura real.
- Los tests que arrancan JavaFX (`ViewsLoadTest`, `ExploreViewsLoadTest`,
  `MenuNavegacionTest`, `DriverDetailControllerTest`,
  `ExploreDriversControllerTest`) corren en modo *headless* sin problema.

## Carga de FXML

`ViewsLoadTest` (9 vistas) y `ExploreViewsLoadTest` verifican que los 20 FXML
cargan y que `fx:controller`, `fx:id` y `fx:include` resuelven. Verde.

## Limitación conocida de este entorno

No se puede completar aquí la comprobación real de MySQL que piden las Fases
1 y 3 (guardar una sesión y releerla desde una instancia MySQL viva). Antes de
dar por cerrada una fase que dependa de MySQL, hay que repetir:

```bash
mvn clean test                              # deben pasar también los 4 de MySqlPersistenceAdapterTest
./run.sh                                    # simulación completa + revisión de Historial
```

en una máquina con `database/SQL/schema.sql` y `database/SQL/data.sql`
aplicados y el servidor arriba.

## Veredicto

Compila, la suite tiene resultado conocido y reproducible, y los FXML cargan.
Falta únicamente la verificación contra MySQL real, documentada arriba como
pendiente de entorno — no como riesgo de código.
