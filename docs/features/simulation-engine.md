# feature/simulation-engine

## Qué se implementó

El núcleo del motor de simulación (RNF-39: `SimulationEngine` ejecuta, `PerformanceCalculator` calcula rendimiento, `LapCalculator` calcula tiempos) y su punto de entrada único.

## Clases

- `LapCalculator`: `calculateLapTime(Driver, Vehicle, Circuit, Weather, SimulationConfig)` — HU-16.
- `PerformanceCalculator`: `calculateTopSpeed`, `calculateTireWear`, `calculateFuelConsumption` — HU-15.
- `SimulationEngine`: `runQualifying(...)` orquesta `LapCalculator` + `PerformanceCalculator` para producir la clasificación (HU-17/HU-18). Recibe ambos calculadores por constructor (inyección manual, sin framework DI).
- `SimulationFacade implements service.SimulationService`: **[patrón Facade]** — único punto de entrada para el controller de simulación. `startQualifying(SimulationConfig)` resuelve piloto/vehículo/circuito vía sus servicios (lanzando `InvalidSimulationException` si no existen), genera el clima con `WeatherService`, y delega en `SimulationEngine`. `getResults(String)` lee de `ResultRepository` de forma segura (captura y devuelve `List.of()` si falla, pues puede consultarse antes de haber corrido ninguna simulación).

## Patrón de diseño

**Facade** — `SimulationController` (siguiente feature) solo conocerá `SimulationFacade`, no los servicios ni el motor por separado.

## Pendiente

`LapCalculator.calculateLapTime`, los tres métodos de `PerformanceCalculator` y `SimulationEngine.runQualifying` lanzan `UnsupportedOperationException` — son la fórmula conceptual de rendimiento (piloto + vehículo + circuito + clima + configuración → tiempo de vuelta) descrita en el `.md`, y su implementación real es la siguiente prioridad de negocio tras completar esta estructura.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 57 archivos fuente compilados.
