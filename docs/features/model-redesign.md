# feature/model-redesign

## Por qué

El modelo heredado no encajaba con la estructura de datos de `f1project.md`: `Vehicle.rendimiento` era un `double` plano cuando la spec define **3 modos de conducción × (velocidad media + consumo por clima + desgaste por clima)**, `Circuit` guardaba los ganadores como `List<String>` sin temporada, y la presión de neumáticos era un `double` cuando la rúbrica la pide categórica.

## Modelo nuevo

### Claves naturales
Se usan las del JSON en lugar de UUIDs sintéticos: `Driver.id` es el entero 1..20 al que apuntan `equipos.pilotos`, `vehiculos.pilotos` y `circuitos.ganadores.piloto`; `Team`/`Circuit` se identifican por `nombre` y `Vehicle` por `modelo`. Así `Driver.equipo` —que en el JSON es un **nombre**— es una clave foránea real sin tabla de traducción.

### `Vehicle.rendimiento` como mapa anidado
```java
Map<DrivingMode, Performance> rendimiento;
Performance { int velocidadPromedioKmh;
              Map<WeatherCondition,Double> consumo, desgaste; }
```
La alternativa plana serían **21 campos** (`consumoNormalSeco`, `consumoNormalLluvioso`, …) con sus getters. Con mapas el acceso en la fórmula es directo y **sin un solo `switch`**: `vehiculo.rendimientoDe(modo).consumoCon(clima)`. Además pone el `Map` en el corazón del dominio, que es lo que pide la rúbrica, y no solo como caché.

`rendimientoDe(modo)` cae al modo normal si el vehículo no define ese bloque, para que la simulación no falle por datos incompletos.

### Clases anidadas
`Vehicle.Performance`, `Circuit.LapRecord` y `Circuit.Winner` van como clases estáticas anidadas: solo existen dentro de su dueño y ahorran 3 archivos de nivel superior.

### `Circuit`: 4 campos nuevos
`probabilidadClima`, `factorTecnico`, `factorConsumo` y `factorDesgaste`. Resuelven tres HU («clima promedio del circuito», «impacto del circuito en desgaste y consumo») y alimentan la fórmula.

**`calcularFactorTecnico()` deriva la sinuosidad del récord real que ya trae el JSON**, en lugar de inventar constantes:
```
factorTecnico = record_segundos / (3600 × longitud_km / 340)
```
→ Mónaco **1,98**, Monza **1,32**. Que «Mónaco es más lento que Monza» sale de los datos de la spec, no de un número mágico. Verificado en `CircuitTest`.

### Enums con sus factores
Cada enum de configuración lleva sus propios coeficientes, de modo que la fórmula no necesita tablas externas ni `switch`:

| Enum | Factores |
|---|---|
| `WeatherCondition(SECO, LLUVIOSO, EXTREMO)` | tiempo 1.000 / 1.080 / 1.180 |
| `AerodynamicLoad(BAJA, MEDIA, ALTA)` | tiempo, consumo, desgaste |
| `TirePressure(BAJA, ESTANDAR, ALTA)` | tiempo, desgaste |
| `FuelStrategy(AGRESIVA, BALANCEADA, AHORRO)` | tiempo, consumo |

**Cada opción tiene contrapartida** (aero baja mejora consumo pero penaliza tiempo; presión baja mejora tiempo pero dispara el desgaste), para que configurar importe de verdad.

### Mapeo Jackson
Los enums llevan `@JsonValue`/`@JsonCreator` con la clave literal de la spec (`conduccion_normal`, `seco`, `Líder`…), para leer el seed sin traducciones ambiguas. El adaptador JDBC usa las mismas claves en los catálogos SQL. `Circuit.LapRecord` expone `tiempo` como `"1:10.166"` pero lo guarda en segundos, porque se compara con los tiempos simulados.

## Utilidades

- `FormatUtils.parseLapTime` (nueva, inversa de `formatLapTime`) y `formatGap`.
- `ValidationUtils.isValidDriverNumber` se elimina: queda huérfana al desaparecer `Driver.numero` (la spec no asigna dorsales).

## Nota sobre los servicios

`DriverServiceImpl`, `CircuitServiceImpl` y `VehicleServiceImpl` recibieron ajustes **mínimos** para seguir compilando tras el cambio de getters. Se reescriben por completo en `feature/catalog-services`.

## Verificación

`mvn clean test` → **10 tests, 0 fallos**:
- `CircuitTest`: el factor técnico derivado da Mónaco 1,98 y Monza 1,32, y Mónaco > Monza.
- `VehicleJsonTest`: lee el formato **literal** de la especificación, cae a modo normal si falta un bloque, y sobrevive al round-trip de serialización. Este test fija el contrato de Jackson con **enums como claves de mapa**, que usa un mecanismo distinto al de los valores y sería un fallo silencioso si se rompiera.
- `FormatUtilsTest`: parseo con y sin minutos, entrada vacía, y que `format`/`parse` son inversas.
