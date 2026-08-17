# HU-33 — Comparar sectores

## Resultado

HU-33 queda implementada mediante una pestaña **Comparación de sectores** dentro de la sesión. La tabla muestra los parciales S1, S2 y S3 de todos los pilotos, el tiempo total y señala automáticamente en verde al ganador de cada sector.

Las vueltas `INVALID` o `OUT` permanecen visibles para conservar la parrilla completa, pero no reciben parciales ni pueden ser consideradas como mejores sectores.

## Modelo y cálculo

- `SectorTimes` es un objeto de valor inmutable: agrupa tres tiempos positivos y finitos, permite consultar un sector y calcula su total.
- `SectorTimeCalculator` distribuye el tiempo base según el factor climático de los 20 segmentos y aplica al sector correspondiente el impacto temporal de cada evento.
- Los parciales se normalizan contra el resultado oficial de `EventEffectService`; por ello, S1 + S2 + S3 siempre coincide con el tiempo final de la vuelta.
- `LapResult` conserva los parciales junto con el resultado. El campo es opcional para mantener compatibilidad con sesiones guardadas antes de HU-33.

## Comparación e interfaz

`SectorComparisonService` contiene la regla para elegir el menor parcial válido y no depende de JavaFX. `SectorComparisonController` solamente presenta los resultados:

1. recibe la parrilla consolidada;
2. solicita el ganador de cada sector al servicio;
3. llena la tabla de parciales;
4. muestra el nombre y tiempo de los tres ganadores;
5. resalta las celdas ganadoras.

La vista está aislada en `sector-comparison.fxml` y se incluye desde `simulation.fxml`, siguiendo el mismo diseño utilizado por HU-32.

## Persistencia y compatibilidad

- Jackson serializa `SectorTimes` dentro de cada `LapResult`.
- Una sesión nueva conserva sus parciales después de serializar y deserializar.
- Un resultado histórico sin `sectorTimes` continúa cargando normalmente y se representa con `—`.

## Criterios de aceptación

| Criterio | Evidencia |
|---|---|
| Comparar pilotos | Tabla con toda la parrilla y columnas S1, S2, S3 y total. |
| Detectar ventajas | Menor tiempo de cada sector identificado automáticamente. |
| Coherencia con la vuelta | La suma de parciales coincide con el tiempo oficial. |
| Clima y eventos reales | El cálculo consume la evolución climática y asigna cada delta al sector donde ocurrió. |
| Vueltas inválidas | No participan en la selección del mejor sector. |
| Persistencia | Los parciales forman parte de `LapResult` y sobreviven al ciclo JSON. |
| Compatibilidad | Las sesiones antiguas sin parciales siguen siendo válidas. |

## Verificación

- Compilación de producción y pruebas con Java 17: correcta.
- JUnit: **79 pruebas, 79 correctas, 0 fallos**.
- La suite verifica invariantes, ganadores distintos por sector, exclusión de vueltas inválidas, suma exacta, persistencia JSON, compatibilidad histórica y carga real de los doce FXML.
