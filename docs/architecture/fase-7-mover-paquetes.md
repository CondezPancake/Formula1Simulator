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

El siguiente lote razonable es **puertos** (riesgo bajo, sin lógica) seguido
de **dividir `service/`** — es donde vive el verdadero trabajo de esta fase,
porque hasta ahora todo lo movido eran paquetes completos.
