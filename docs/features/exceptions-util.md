# feature/exceptions-util

## Qué se implementó

Excepciones de dominio (RNF-33) y utilidades transversales (RNF-36) usadas por el resto de capas.

## Clases

`com.formula1.exception`:
- `InvalidDriverException`, `InvalidVehicleConfigurationException`, `InvalidSimulationException`, `OpenF1ConnectionException`, `DatabaseException` — todas `extends RuntimeException`, con constructor `(String message)` y `(String message, Throwable cause)`. No son checked para no forzar `throws` en cascada en un proyecto de alcance acotado.

`com.formula1.util`:
- `DateUtils` — `now()`, `format(LocalDateTime, [pattern])`.
- `ValidationUtils` — `isNotBlank`, `isPositive`, `isInRange`, `isValidDriverNumber` (soporta RNF-07: velocidad > 0, longitud > 0, vueltas > 0, nombre ≠ vacío).
- `FormatUtils` — `formatLapTime` (segundos → `"1:23.456"`), `formatPercentage`.
- `MathUtils` — `clamp`, `average`, `percentageOf`.
- `RandomUtils` — `randomDouble`, `randomInt`, `randomBoolean`, `pickRandom` (base para variabilidad del motor de simulación cuando se implemente).

Todas las clases de `util` tienen constructor privado (no instanciables) y responsabilidad única por clase (evitan convertirse en una "God class" de utilidades, según lo pedido en RNF-36).

## Patrón de diseño

No aplica directamente.

## Pendiente

Ninguno de nivel C: son utilidades e infraestructura de excepciones, no lógica de negocio.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 26 archivos fuente compilados.
