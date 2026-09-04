# Fase 5 — convertir JavaFX en adaptador de entrada

## Cambios

**Fábrica de tareas.** `SimulationController` ya no construye el `Task` de
clasificación llamando a `QualifyingService`; llama a la
`QualifyingSessionTaskFactory` de la Fase 4 (`this.tareas`, construida en el
constructor a partir del mismo `QualifyingService` inyectado). El controlador
sigue haciendo exactamente lo que le corresponde: recoger la configuración de
pantalla, lanzar la tarea, enlazar progreso/mensaje/botones a sus
propiedades y reaccionar a `onSucceeded`/`onFailed`.

**Puerto para la configuración preparada.** `ConfigController`,
`HistoryController` y `SimulationController` llamaban a
`DataStore.getInstance().configuracionActual() / .guardarConfiguracion(...) /
.versionConfiguracion()` directamente — 8 puntos de llamada en total. Se creó
`data.PreparedConfigPort` (puerto de solo estas tres operaciones;
`DataStore` lo implementa sin cambiar su lógica) y los tres controladores
pasan a depender de él:

- Cada controlador ganó un parámetro de constructor
  `PreparedConfigPort configuracionPreparada`; el constructor sin argumentos
  (el que usa `fx:controller`) sigue resolviendo `DataStore.getInstance()`
  para no romper la carga por reflexión de FXML.
- Ningún controlador quedó con una llamada a `DataStore` que no sea esa
  única línea de compatibilidad en su constructor sin argumentos.

**Verificado**: `grep DataStore` sobre los tres archivos solo encuentra la
línea del constructor por defecto en cada uno.

## Qué se dejó fuera, deliberadamente

Dos usos de `DataStore` no se tocaron en esta fase:

1. **`ShellController.cargar()/estaCargado()`** — arranque de la aplicación
   (decide si sembrar desde MySQL o desde el JSON). Es responsabilidad de
   arranque/composición, no de un caso de uso; encaja con la Fase 6
   ("composición central de dependencias"), no con esta.
2. **`MainMenuController.rellenarHud()`** — lee `estaCargado()` +
   `pilotos()/equipos()/circuitos()` para tres contadores decorativos del
   menú. Mezcla una bandera de arranque (no cubierta por ningún puerto) con
   lecturas de catálogo (sí cubiertas por `CatalogPort` desde la Fase 2). Se
   dejó igual para no partir esa única línea en dos fuentes de datos por un
   HUD decorativo de bajo riesgo; es la extracción más pequeña que queda
   pendiente si se quiere cerrar del todo el punteo de accesos directos.

Ninguno de los dos accede a JDBC directamente ni contiene una regla de
negocio: ambos son lectura de estado de arranque, que es exactamente lo que
`DataStore` sigue siendo libre de exponer mientras no llegue la Fase 6.

## Riesgos de la fase — verificados

- **Actualizaciones fuera del hilo de JavaFX**: no aplica — no se tocó cómo
  se publican progreso/mensaje (`Task` sigue haciéndolo internamente).
- **Progreso, cancelación o finalización**: `finalizarSolicitado`,
  `onSucceeded`, `onFailed` y el enlace de propiedades no cambiaron una
  línea.
- **Handlers `@FXML` rotos**: los métodos y campos `@FXML` no cambiaron de
  nombre ni de firma; solo constructores no anotados con `@FXML` ganaron
  parámetros, y todos con un constructor sin argumentos equivalente que
  usa FXML.
- **Pérdida de estado de sesión al navegar**: `Navigator` no se tocó en esta
  fase; el estado sigue viviendo donde vivía.

## Verificación

- `mvn clean test`: 172/0/0, 4 *skipped* (MySQL) — misma cuenta que en Fase 4.
- `ViewsLoadTest` (9), `ExploreViewsLoadTest` (1) y `MenuNavegacionTest` (3):
  verdes — los 20 FXML cargan y los controladores se construyen por
  reflexión sin error, confirmando que los constructores sin argumentos
  siguen siendo compatibles con `fx:controller`.

## Veredicto

`SimulationController` ya no construye el `Task` de simulación ni contiene
la lógica que lo hacía; `ConfigController`, `HistoryController` y
`SimulationController` dependen de `PreparedConfigPort`, no de
`DataStore.getInstance()`, salvo en su constructor de compatibilidad con
FXML. Quedan pendientes, para una fase posterior, los dos usos de arranque
señalados arriba y la composición central (Fase 6), que es donde ese último
`DataStore.getInstance()` en cada constructor por defecto debería
desaparecer también.
