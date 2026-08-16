# refactor/scope-cleanup

## Por qué

Apareció `f1project.md`, que es **la rúbrica real** del proyecto y pide algo mucho más simple que `ProyectoFormula1.md` (el documento que se siguió en la primera iteración). Esta rama recorta todo lo que se construyó de más y que la rúbrica no pide, dejando el proyecto compilando en cada paso.

## Cambios en documentación

- `f1project.md` pasa a estar versionado y es **la especificación autoritativa**.
- `ProyectoFormula1.md` se mueve a `docs/legacy/` — se conserva como referencia histórica, pero ya no es la referencia de trabajo.

## Qué se eliminó (20 archivos `.java`)

| Elemento | Motivo |
|---|---|
| `api/F1DataProvider`, `api/OpenF1ApiAdapter` | **OpenF1 no aparece ni una vez en `f1project.md`** → se elimina el patrón **Adapter** |
| `service/OpenF1Service` + `Impl` | Sin API externa que envolver |
| `simulation/` completo (`LapCalculator`, `PerformanceCalculator`, `SimulationEngine`, `SimulationFacade`) | 4 clases para un cálculo que cabe en 2. `SimulationFacade` no fachadeaba un subsistema, era un servicio de aplicación → se elimina el patrón **Facade** |
| `service/SimulationService` | Se reemplazará por `QualifyingService` en `feature/qualifying-engine` |
| `service/WeatherService` + `Impl` | Generar el clima es elegir entre 3 estados: interfaz + impl para un método es ceremonia |
| `model/Weather` (clase de 6 campos) y `model/TrackStatus` (5 estados) | La rúbrica define el clima como **3 estados** (seco / lluvioso / extremo), que son además las claves reales del JSON. Se reemplazan por un enum en `feature/model-redesign` |
| `model/SessionPhase` (Q1/Q2/Q3) | No existen fases en la rúbrica: hay *una* sesión de clasificación |
| `model/TireCompound` | No hay compuestos en la rúbrica; sí hay *presión* de neumáticos, que es otra cosa |
| `model/Simulation`, `model/Result` | Arrastraban `fase`, `sectores` y `neumaticoUsado`. Se reemplazan por `QualifyingSession` y `LapResult` |
| `repository/ResultRepository` + `Impl` | Dependían de `Result` |
| `exception/OpenF1ConnectionException` | Sin OpenF1 |

## Otros cambios

- `pom.xml`: se quita `jackson-datatype-jsr310` (no hay fechas serializadas a JSON). **`jackson-databind` se conserva** porque lo necesitan el seed de datos y el mapeo entidad↔`Document`.
- `SimulationController`: se reduce a un enlace mínimo con la vista, ya que las clases de las que dependía desaparecieron. Su implementación real llega en `feature/ui-simulation`.

## Patrones de diseño

De **4 a 2**: mueren **Adapter** (con `api/`) y **Facade** (con `SimulationFacade`). Sobreviven **Repository** y **Singleton**, ambos consecuencia directa de mantener MongoDB.

## Resultado

64 → **44 archivos** `.java`; 10 → **8 paquetes** (la reducción a 5 se completa en `feature/data-layer` y `feature/catalog-services`).

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 44 archivos compilados.
