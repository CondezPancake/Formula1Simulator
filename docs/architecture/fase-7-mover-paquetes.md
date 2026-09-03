# Fase 7 — mover paquetes gradualmente

Esta fase se ejecuta en lotes, cada uno con su propia verificación completa,
tal como pide el diagnóstico ("un grupo por paso autorizado"). Este archivo
se va a actualizar en cada lote.

## Lote 1 — dominio puro: `model` → `domain.model`, `event` → `domain.event`

El diagnóstico pide mover primero el dominio puro, así que es el primer
lote. Ambos paquetes se movieron completos y atómicos (nada se dividió
dentro de ellos), lo que los hace el movimiento de menor riesgo posible:

- `com.formula1.model` (37 clases) → `com.formula1.domain.model`
- `com.formula1.event` (14 clases) → `com.formula1.domain.event`

**Mecánica**: `git mv` archivo por archivo (conserva el historial), cambio
de la declaración `package` en cada uno, y una sustitución global de
`com.formula1.model.` → `com.formula1.domain.model.` y
`com.formula1.event.` → `com.formula1.domain.event.` sobre **todo**
`simulator/src` (producción y test — 88 archivos referenciaban `model`, 5
referenciaban `event`). Los tests de esas dos carpetas también se movieron a
`test/java/com/formula1/domain/{model,event}` con su `package` corregido —
un test declarado en el paquete viejo dejaba de ver por acceso de paquete
las clases que ya se habían mudado, y eso lo detectó el compilador, no en
tiempo de ejecución.

No hizo falta tocar ninguna visibilidad: ambos paquetes se movieron enteros,
así que las relaciones `package-private` internas (dentro de `model`, dentro
de `event`) se conservan intactas en el paquete nuevo.

**Verificación**: `mvn clean compile` en verde a la primera pasada para
producción; el primer `mvn clean test` sí falló —exactamente en los tests
movidos sin corregir, con error de compilación, no en tiempo de ejecución—,
se corrigió y quedó en `174/0/0`, 4 *skipped* (MySQL), misma cuenta que
Fase 6. Cero cambios de comportamiento: es un movimiento mecánico puro.

## Qué falta de esta fase

Según la tabla del diagnóstico, quedan por mover:

| Grupo | Destino | Complejidad |
|---|---|---|
| Calculadoras y políticas puras dentro de `service/` (`LapTimeCalculator`, `TelemetryCalculator`, `SectorTimeCalculator`, `*PitStopPolicy`, `*TireCompoundPolicy`, `DynamicWeatherService`, `TrackEvolutionService`, `SectorComparisonService`...) | `domain/service` | Media — hay que **dividir** el paquete `service`, no moverlo entero: separar cálculo puro de orquestación. |
| Orquestación (`QualifyingService`, `QualifyingSessionTaskFactory`, `DriverService`, `TeamService`, `VehicleService`, `CircuitService`) | `application/usecase` | Media — depende del resultado del punto anterior para no romper accesos de paquete entre ambos grupos. |
| Puertos (`CatalogPort`, `SessionHistoryPort`, `QualifyingDataPort`, `PreparedConfigPort`, `CatalogPersistencePort`, `SessionPersistencePort`, `PersistencePort`) | `application/port/out` | Baja — son interfaces, sin lógica que dividir. |
| Adaptadores MySQL (`MySql*`, `DatabaseConnection`, `JdbcTransactionSupport`) y `DataStore` | `adapter/out/mysql` (los MySql) + decisión pendiente para `DataStore` (sigue siendo el punto crítico que señalaba el diagnóstico) | Media-alta — `DataStore` es el mayor riesgo de toda la fase por ser el más referenciado. |
| Seeds (`SeedLoader`) | `adapter/out/seed` | Baja |
| Controladores, presenters, `Navigator`, tareas JavaFX | `adapter/in/javafx` | Alta — 18 archivos FXML referencian el nombre de paquete completo vía `fx:controller`; hay que actualizar los 20 FXML a la vez que las clases, y es el único movimiento que un test de carga de FXML (`ViewsLoadTest`) puede pillar en tiempo de ejecución en vez de en compilación. El diagnóstico pide explícitamente moverlo al final. |
| `App`, `Main`, `AppComposition` | `bootstrap` | Baja |
| `util/` | Revisar una por una — el propio diagnóstico dice que no todas pertenecen a la misma capa (p. ej. `Async` es infraestructura, `MathUtils`/`FormatUtils` son utilidades de dominio puras). | Media, por la revisión caso a caso, no por el movimiento en sí. |

## Lote 2 — puertos: `data.*Port` → `application.port.out`

Las 7 interfaces sin lógica (`CatalogPort`, `SessionHistoryPort`,
`QualifyingDataPort`, `PreparedConfigPort`, `CatalogPersistencePort`,
`SessionPersistencePort`, `PersistencePort`) se mudan de `com.formula1.data`
a `com.formula1.application.port.out`.

A diferencia del lote 1, las implementaciones de estas interfaces
(`DataStore`, `MySqlPersistenceAdapter` y sus dos adaptadores por puerto) se
quedan en `com.formula1.data` y las referenciaban por nombre simple, al
compartir paquete hasta ahora. El primer `mvn clean compile` lo marcó con
precisión quirúrgica (3 errores, "cannot find symbol") y se corrigió
agregando el import explícito en cada implementación — siguen implementando
los mismos puertos, ahora cruzando paquete.

`mvn clean test`: 174/0/0, 4 *skipped* — cero regresiones.

## Lote 3 — dividir `service/` en `domain/service` y `application/usecase`

El lote de verdadero trabajo de esta fase: por primera vez no se mueve un
paquete entero, sino que se **reparten** sus 21 clases entre dos capas según
su naturaleza real, siguiendo el criterio que ya usaba el diagnóstico
("Los servicios de cálculo y las políticas... están más cerca de
responsabilidades de dominio independientes"):

**→ `domain/service`** (cálculo puro y reglas, sin JavaFX, sin acceso a
catálogo vía puerto): `ContextualPitStopPolicy`, `ContextualTireCompoundPolicy`,
`DynamicWeatherService`, `LapTimeCalculator`, `PitStopPolicy`, `PitStopService`,
`RaceNarratorService`, `RaceRadioService`, `SectorComparisonService`,
`SectorTimeCalculator`, `TelemetryCalculator`, `TireCompoundPolicy`,
`TireStrategyService`, `TrackEvolutionService`, y `ValidationException`
(ver nota abajo).

**→ `application/usecase`** (orquestación, CRUD de catálogo vía puerto):
`CircuitService`, `DriverService`, `TeamService`, `VehicleService`,
`QualifyingService`.

**Se queda en `service`** (deliberado, para el lote de JavaFX): 
`QualifyingSessionTaskFactory`, `SimulationPacer` — siguen envolviendo
`QualifyingService` en un `Task`, así que van junto a los controladores en
el próximo lote, tal como ya anticipaba la Fase 4.

### Un error de diseño corregido sobre la marcha

`ValidationException` se movió primero a `application/usecase` (donde vive
la mayoría de quien la lanza) y el primer `mvn clean compile` lo rechazó:
`LapTimeCalculator`, `DynamicWeatherService` y `TrackEvolutionService`
—todas en `domain/service`— también la lanzan, y `domain` **no puede**
depender de `application` en arquitectura hexagonal; es la dirección
contraria a la permitida. Se corrigió moviéndola a `domain/service` en su
lugar: `application/usecase` sí puede depender de `domain/service` (la capa
de aplicación depende del dominio, nunca al revés), así que los cuatro
servicios de catálogo y `QualifyingService` la siguen lanzando sin problema
desde la capa de arriba. El compilador detectó la dirección de dependencia
incorrecta en el primer intento, antes de que llegara a ningún commit.

### Visibilidad ensanchada

Dividir el paquete (en vez de moverlo entero, como el lote 1) sí exige
ensanchar visibilidad, porque `QualifyingService` construía e invocaba
varias de estas clases confiando en compartir paquete:

- `DynamicWeatherService`, `SectorTimeCalculator`, `TelemetryCalculator`,
  `TrackEvolutionService` — las 4 clases enteras eran *package-private*;
  pasan a públicas.
- `DynamicWeatherService(RandomGenerator)`, `DynamicWeatherService.generar(...)`,
  `TrackEvolutionService.evolucionar(...)` y su tipo `Evolution`,
  `SectorTimeCalculator.calcular(...)`, `TelemetryCalculator.calcular(...)`
  — miembros concretos que eran *package-private* dentro de clases ya
  públicas; el ensanchamiento de clase no ensancha sus miembros
  automáticamente, así que cada uno se corrigió por separado, guiado otra
  vez por el error exacto del compilador.
- En `QualifyingService` (ahora en `application.usecase`), los mismos 4
  miembros que ya se habían ensanchado en la Fase 4 para
  `QualifyingSessionTaskFactory` (el `simular(...)` de 11 parámetros,
  `validarSeleccion`, `ControlSimulacion`, `SEGMENTOS_EVOLUCION`) necesitaban
  ensancharse una vez más, esta vez a `public` de verdad: `service` y
  `application.usecase` ya no comparten paquete.

### Migración de tests

Los tests que probaban directamente una clase *package-private* por
compartir paquete (`DynamicWeatherServiceTest`, `TrackEvolutionServiceTest`,
`RaceRadioServiceTest`, `SectorComparisonServiceTest`) se movieron a
`test/.../domain/service`; los que probaban orquestación
(`CatalogPortDecouplingTest`, `CatalogServiceTest`, `EvolutionDataFlowTest`,
`QualifyingServiceTest`, incluido `VehicleSpeedTest`, que llama al
`calcularVelocidad` *package-private* de `QualifyingService`) se movieron a
`test/.../application/usecase`. `SimulationPacerTest` se quedó en
`test/.../service`, junto a la clase que prueba.

**Verificación**: `mvn clean test`: 174/0/0, 4 *skipped* (MySQL) — misma
cuenta que los lotes anteriores, cero regresiones, a pesar de ser el lote
con más superficie de cambio hasta ahora (21 clases repartidas, 10 miembros
ensanchados, 9 archivos de test reubicados).

## Lote 4 — adaptadores MySQL, seed y `DataStore`

`com.formula1.data` (10 clases) se reparte en tres destinos, resolviendo la
decisión que el diagnóstico original dejaba abierta sobre dónde va
`DataStore`:

- **`adapter/out/mysql`**: `MySqlPersistenceAdapter`,
  `MySqlCatalogPersistenceAdapter`, `MySqlSessionPersistenceAdapter`,
  `MySqlCatalogRepository`, `MySqlSessionRepository`, `DatabaseConnection`,
  `JdbcTransactionSupport` — el JDBC puro, tal como pedía la tabla del
  diagnóstico.
- **`adapter/out/seed`**: `SeedLoader`.
- **`adapter/out/memory`**: `DataStore`. El diagnóstico no lo ubicaba —era,
  literalmente, "el punto crítico" sin destino asignado. Se decide aquí:
  `DataStore` es un adaptador de salida como los otros dos (implementa
  `CatalogPort`, `SessionHistoryPort`, `QualifyingDataPort`,
  `PreparedConfigPort`), solo que en memoria y con `MySqlPersistenceAdapter`
  detrás como persistencia duradera — de ahí que viva junto a `mysql/` y
  `seed/` bajo `adapter/out`, en su propio subpaquete porque es una
  responsabilidad distinta (caché + orquestación de fallback), no
  intercambiable con los otros dos.
- **`adapter/out/DataAccessException`**: se queda en la raíz de `adapter.out`
  porque la lanzan los tres grupos (`mysql`, `seed`, y `DataStore` mismo)
  — es la excepción compartida de toda la capa de adaptadores de salida, no
  de uno solo.

Mecánica idéntica a los lotes anteriores: mover, corregir `package`,
sustituir referencias completas, y siete importaciones nuevas para
referencias que compartían paquete con nombre simple (`DataStore` usaba
`SeedLoader`, `DatabaseConnection`, `MySqlPersistenceAdapter` y
`DataAccessException` así; los cuatro adaptadores/repos JDBC y `SeedLoader`
usaban `DataAccessException` así). Un solo test
(`MySqlPersistenceAdapterTest`, movido a `test/.../adapter/out/mysql`)
necesitó un import adicional para `DataStore.enMemoria()`.

`mvn clean test`: 174/0/0, 4 *skipped* (MySQL) — misma cuenta, cero
regresiones. `com.formula1.data` queda vacío y eliminado.

## Lote 5 — controladores, `Navigator` y las tareas JavaFX → `adapter/in/javafx`

El lote que el diagnóstico pedía dejar para el final ("mover JavaFX al
final"), y con razón: es el único que toca los 20 FXML a la vez que las
clases. Se movieron juntas, como un solo paquete completo (sin dividir,
igual que el lote 1):

- Las 30 clases de `com.formula1.controller` (controladores por pantalla,
  `Navigator`, `ShellController`, los presenters de telemetría/radio/paradas/
  neumáticos, y los componentes visuales `CircuitoEnVivo`, `MapaProgreso`,
  `Forms`, `AjustesDialog`, `ExploreCardVisuals`).
- `QualifyingSessionTaskFactory` y `SimulationPacer`, que se habían quedado
  deliberadamente en `service/` desde la Fase 4 esperando este momento —
  ahora sí les toca vivir junto al resto de la infraestructura JavaFX, tal
  como anticipaba su propio comentario de entonces.

Los 20 archivos FXML se actualizaron en el mismo paso: los 18
`fx:controller="com.formula1.controller.X"` pasan a
`fx:controller="com.formula1.adapter.in.javafx.X"`. Ningún FXML usa
`<?import com.formula1.controller...?>` para componentes personalizados, así
que no hubo un segundo tipo de referencia que corregir ahí.

**Resultado**: `mvn clean compile` en verde a la primera pasada — sin un
solo error de "cannot find symbol". A diferencia del lote 3 (dividir
`service/`), aquí no hizo falta ensanchar ninguna visibilidad: al moverse el
paquete completo (controladores + `Navigator` + presenters + las dos clases
que ya esperaban desde la Fase 4), todas las relaciones internas que
dependían de compartir paquete siguen compartiéndolo en el destino nuevo.

**Verificación**: `mvn clean test`: 174/0/0, 4 *skipped* (MySQL) — misma
cuenta. Es la comprobación más fuerte de toda la Fase 7 hasta ahora, porque
`ViewsLoadTest` (9 tests) carga los 20 FXML de verdad con `FXMLLoader.load(...)`
y `MenuNavegacionTest` (3 tests) ejercita `Navigator.ir(...)` en vivo: si el
`fx:controller` de algún FXML se hubiera quedado apuntando al paquete viejo,
estos tests habrían fallado en tiempo de ejecución con
`ClassNotFoundException`, no en compilación. Pasaron limpio.

`com.formula1.controller` y `com.formula1.service` quedan vacíos y
eliminados — no queda ningún paquete de la estructura original salvo
`model`/`event` (ya movidos en el lote 1) y `data` (ya movido en el lote 4).

## Lote 6 — `App`, `Main`, `AppComposition` → `bootstrap`

Las tres clases de arranque y composición se mueven de la raíz
`com.formula1` a `com.formula1.bootstrap`. Sin sorpresas de código: el
`import` de cada clase de `AppComposition` ya apuntaba a los paquetes
nuevos desde los lotes anteriores.

**La sorpresa estaba fuera del código fuente**, donde ningún test la puede
ver: `simulator/pom.xml` (el plugin `javafx-maven-plugin`, usado por
`mvn javafx:run` y `run.sh`) y `.vscode/launch.json` (el botón *Run* de VS
Code, documentado en el propio `README.md`) tenían el nombre de clase
completo (`com.formula1.App`, `com.formula1.Main`) escrito como texto, no
como código Java — `mvn clean test` nunca los toca. Se corrigieron los dos
a mano. Para verificar sin depender solo de la lectura, se corrió
`mvn javafx:run` con límite de tiempo: si el `mainClass` hubiera quedado
mal, Maven habría fallado en segundos con `ClassNotFoundException`; en vez
de eso compiló, copió recursos y el proceso quedó corriendo hasta que el
límite de tiempo lo cortó (código 143, SIGTERM) — el patrón esperado de un
arranque que sí encontró la clase y llegó a levantar JavaFX.

`mvn clean test`: 174/0/0, 4 *skipped* (MySQL) — misma cuenta.

## Lote 7 — revisión de `util/`

El propio diagnóstico advertía que `util/` no pertenece entero a una sola
capa. Revisadas las 15 clases una por una:

| Clase | Depende de | Veredicto |
|---|---|---|
| `MathUtils`, `RandomUtils`, `DateUtils`, `ValidationUtils`, `FormatUtils` | Nada fuera de `java.*` | Utilidades puras — candidatas a `domain` si se quisiera separar, pero son transversales por diseño (las usa dominio y JavaFX por igual) |
| `Async` | `java.util.concurrent` | Infraestructura de hilos, no específica de JavaFX ni de MySQL |
| `TeamColors`, `TrackLayouts`, `VehicleImages`, `F1Assets`, `Imagenes`, `ImageCrop` | Rutas de recursos, algunas con `javafx.scene.image.Image` | Infraestructura de presentación |
| `AudioManager`, `TtsManager` | Audio/TTS, usadas por los presenters de `adapter.in.javafx` | Infraestructura de presentación |
| `InputValidation` | — | Validación de formularios de UI |

Comprobado por import: solo 3 de las 15 dependen de algo fuera de
`java.*`/`util`, y las 3 apuntan a `domain` (`FormatUtils`→`domain.model.LapResult`,
`InputValidation`→`domain.service.ValidationException`,
`TrackLayouts`→`domain.model.TrackLayout`) — la dirección permitida, nunca
hacia `application` ni `adapter`. Así que **ninguna crea un ciclo ni una
dependencia invertida quedándose donde está**: son transversales
(`MathUtils` et al.) o infraestructura de presentación (imágenes, audio,
hilos), con como mucho una lectura hacia el dominio. Dividir `util/` en
`domain/util` +
`adapter/in/javafx/util` sin que ningún consumidor lo exija todavía sería
mover archivos por mover — no hay una razón de dependencia que lo pida, a
diferencia de los lotes anteriores, donde cada movimiento resolvía una
violación de dirección real (dominio dependiendo de aplicación, casos de
uso dependiendo de JavaFX). Se documenta la revisión y se deja `util/` como
está: es una decisión, no un olvido.

## Fase 7 — cierre

Los 6 lotes de movimiento (dominio, puertos, división de `service/`,
persistencia, JavaFX, arranque) están completos y verificados; el lote 7
es una revisión que concluye sin cambios. Estructura final de paquetes:

```
com.formula1
├── bootstrap        (App, Main, AppComposition)
├── domain
│   ├── model         (37 clases)
│   ├── event          (14 clases)
│   └── service        (15 clases: cálculo puro, políticas, ValidationException)
├── application
│   ├── usecase         (5 clases: QualifyingService + 4 servicios de catálogo)
│   └── port.out          (7 interfaces)
├── adapter
│   ├── in.javafx        (32 clases: controladores, Navigator, presenters, tareas)
│   └── out
│       ├── mysql          (7 clases JDBC)
│       ├── seed            (SeedLoader)
│       ├── memory           (DataStore)
│       └── DataAccessException
└── util               (15 clases transversales, sin dividir — ver lote 7)
```

Cada lote quedó verificado con `mvn clean test` en 174/0/0 (4 *skipped* por
falta de MySQL local) y, en el lote 5, además con la carga real de los 20
FXML. Ningún lote dejó una regresión sin corregir antes del siguiente commit.
