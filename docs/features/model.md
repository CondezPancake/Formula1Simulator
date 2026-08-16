# feature/model

## Qué se implementó

Modelo de dominio del núcleo MVP (`com.formula1.model`), cubriendo las entidades de HU-01 a HU-04 (equipos, pilotos, vehículos, circuitos) y las de configuración/ejecución de una simulación (HU-07 a HU-18).

## Clases

- `Team`, `Driver`, `Vehicle`, `Circuit`: entidades de gestión (RF-01 a RF-04), atributos privados con getters/setters, `equals`/`hashCode` por `id` y `toString`.
- `Weather`: condiciones climáticas de una sesión (RF-11).
- `SimulationConfig`: selección de circuito/piloto/vehículo + configuración de conducción, aerodinámica, neumáticos y combustible (RF-06 a RF-10).
- `Simulation`: agrega `SimulationConfig`, `Weather`, fase (`SessionPhase`) y lista de `Result`.
- `Result`: posición, tiempo, sectores y neumático usado tras una clasificación (RF-13 a RF-15).

## Enums

`DriverRole`, `DrivingMode`, `AerodynamicLoad`, `FuelStrategy`, `TrackStatus`, `TireCompound`, `SessionPhase`.

## Patrón de diseño

No aplica directamente (capa de modelo). Cumple RNF-19 (encapsulamiento: todos los atributos `private`) y RNF-21 (clases de dominio con responsabilidad concreta).

## Pendiente

Ninguno de nivel C: estas clases son datos + comportamiento trivial (getters/setters), no contienen lógica de simulación.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 16 archivos fuente compilados.
