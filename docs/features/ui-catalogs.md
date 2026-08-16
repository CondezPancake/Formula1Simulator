# feature/ui-catalogs

## Por qué

Las cuatro vistas de catálogo existían pero eran inservibles: **ninguna `TableView` tenía columnas definidas**, así que salían vacías aunque hubiera datos, y los botones de alta y baja no hacían nada. Tampoco había búsqueda, ni comparación de vehículos, ni ficha de circuito — tres historias de usuario sin cubrir.

## Vistas

| Vista | Contenido |
|---|---|
| `drivers` | 8 columnas (id, nombre, equipo, rol, experiencia y las 3 habilidades) + buscador + CRUD |
| `teams` | Equipo, país, motor, nº de pilotos y sus nombres + buscador + CRUD |
| `vehicles` | Modelo, equipo, motor, velocidades, aceleración y pilotos + búsqueda por características + CRUD + asignación + comparación |
| `circuits` | Circuito, país, longitud, vueltas, récord y factor técnico + buscador + CRUD + ficha |
| `vehicle-compare` | Tabla transpuesta + `BarChart` de velocidad media |
| `circuit-detail` | Récord, ganadores, clima promedio e impacto en consumo/desgaste |

## Decisiones

**Formularios en código, no en FXML.** Los diálogos de alta y edición viven en una única clase `Forms` en lugar de cuatro FXML con sus cuatro controladores: son formularios cortos y muy parecidos, así que agruparlos ahorra **ocho archivos** sin perder claridad. El de vehículos usa un `TabPane` con una pestaña por modo de conducción, que es la forma natural de editar un `Map<DrivingMode, Performance>`.

**Tabla de comparación transpuesta** (una fila por métrica, una columna por vehículo): así las diferencias se leen en horizontal, que es el objetivo de comparar. Con la orientación normal habría que ir saltando entre filas.

**Las claves naturales no se editan.** Nombre de equipo, nombre de circuito y modelo de vehículo aparecen deshabilitados al editar: son el identificador de la entidad, y permitir renombrarlos rompería las referencias de pilotos y resultados.

**La asignación de pilotos solo ofrece los del equipo del vehículo**, y el servicio vuelve a validarlo por si acaso: la HU pide explícitamente asignar «según su equipo».

El buscador filtra **mientras se teclea** (`textProperty().addListener`), sin botón de buscar.

## Verificación

`mvn clean test` → **40 tests, 0 fallos**.

Lo más útil de esta rama es `ViewsLoadTest`, que **carga de verdad las 10 vistas FXML** arrancando el toolkit de JavaFX. Los errores de FXML —un `fx:id` que no existe en el controlador, un `onAction` mal escrito, un import que falta— no los detecta el compilador y solo aparecerían al abrir la pantalla; este test los saca en la construcción. Si no hay entorno gráfico avisa por `stderr` en lugar de dar un falso verde.
