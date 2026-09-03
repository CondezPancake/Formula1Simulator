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

## Lo que falta

El siguiente lote es **controladores/`Navigator`/`QualifyingSessionTaskFactory`/
`SimulationPacer` → `adapter/in/javafx`** (el único que toca los 20 FXML a
la vez que las clases, dejado para el final a propósito), y por último
**`App`/`Main`/`AppComposition` → `bootstrap`** y la revisión caso a caso de
`util/`.
