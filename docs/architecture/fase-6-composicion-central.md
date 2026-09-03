# Fase 6 — composición central de dependencias

## Cambio

`AppComposition` (paquete `com.formula1`, junto a `App`/`Main`) es ahora el
único sitio que resuelve `DataStore.getInstance()` para construir el resto de
la aplicación. Crea una vez cada servicio (`DriverService`, `TeamService`,
`VehicleService`, `CircuitService`, `QualifyingService`) y expone una
`Callback<Class<?>, Object>` — la fábrica de controladores de JavaFX.

Esa fábrica reconoce las 13 clases `fx:controller` que declaran un
constructor con servicios (`CircuitController`, `CircuitDetailController`,
`ConfigController`, `DriverController`, `DriverDetailController`,
`ExploreDriversController`, `ExploreTeamsController`, `HistoryController`,
`SimulationController`, `TeamController`, `TeamDetailController`,
`VehicleCompareController`, `VehicleController`) y les entrega las mismas
instancias compartidas en vez de que cada una construya las suyas — antes,
cada pantalla montaba su propio `new DriverService()`, `new
QualifyingService()`, etc. en su constructor sin argumentos. Para cualquier
otro `fx:controller` (`ShellController`, `MainMenuController`,
`ManagementController`...) cae al mismo mecanismo de siempre: su
constructor sin argumentos, por reflexión.

`App.java` instala esa fábrica en sus dos `FXMLLoader` (menú y shell) y en
`Navigator` (`Navigator.usarFabrica(...)`, llamado una sola vez al arrancar,
antes de la primera navegación) para que las vistas que carga bajo demanda
—las otras 15 de las 18 `fx:controller`— compartan la misma composición.
`Navigator` sigue siendo estático (eso es un rediseño mayor, fuera de
alcance de esta fase); solo ganó un campo estático más para la fábrica,
siguiendo el mismo patrón que ya usaba para `registrar(StackPane)`.

`App.start()` también dejó de llamar a `DataStore.getInstance()`
directamente para la carga inicial: usa `composicion.datos()`.

## Qué NO cambió

- El singleton `DataStore` sigue existiendo — Fase 6 centraliza *quién lo
  pide*, no lo elimina. Eliminarlo sería rediseñar `DataStore` en sí, que es
  deuda ya señalada (ver `fase-6` más abajo, "veredicto") y no estaba en el
  alcance de esta fase.
- Los constructores sin argumentos de controladores y servicios se
  mantienen: siguen resolviendo `DataStore.getInstance()` como respaldo, para
  que un test o una construcción fuera de `AppComposition` no se rompa.
- `ShellController.cargar()/estaCargado()` y el HUD de `MainMenuController`
  (accesos a `DataStore` deliberadamente dejados fuera de la Fase 5) tampoco
  se tocaron aquí — siguen siendo arranque/lectura directa, ahora conviven
  con la composición central sin contradecirla.

## Verificación

Ni `ViewsLoadTest` (usa su propio `FXMLLoader`, no pasa por `Navigator`) ni
`MenuNavegacionTest` (usa `Navigator` pero nunca instala la fábrica) ejercitan
este cambio — sin una prueba nueva, la fábrica de 13 clases habría quedado
sin verificar más allá de "compila". Se añadió `AppCompositionTest`, que
construye cada uno de los 13 controladores con dependencias a través de la
fábrica y confirma que no lanza y que el tipo es el correcto, más un caso de
control (`ManagementController`, sin dependencias) para el camino por
defecto. No necesita el toolkit de JavaFX: los constructores no tocan nodos
de escena, eso lo inyecta `FXMLLoader` después mediante los campos
`@FXML`.

`mvn clean test`: 174/0/0 (172 + 2 nuevos), 4 *skipped* (MySQL) — cero
regresiones.

## Veredicto

Hay un único punto de construcción para servicios y controladores, y los
casos de uso no crean sus propias dependencias — el criterio de cierre de
esta fase. Sigue pendiente, y vale la pena decirlo con la misma franqueza que
el diagnóstico original: `DataStore` en sí mismo sigue siendo un god-object
(singleton + caché + fallback a JSON + selección de adaptador MySQL + cuatro
puertos de aplicación implementados a la vez). Esta fase le puso una fábrica
delante; no lo partió. Partirlo —separar el ciclo de vida del singleton, el
caché en memoria y la orquestación de persistencia en clases distintas que
`AppComposition` ensamble por separado— sería la extensión natural de esta
fase si se quiere cerrar del todo, pero no estaba en las 8 fases originales
del diagnóstico y por eso no se hizo sin pedirlo antes.
