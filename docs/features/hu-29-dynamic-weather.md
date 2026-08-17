# HU-29 — Evolución climática

## Resultado

La condición meteorológica dejó de ser un valor estático. Cada sesión genera 20 muestras suaves que comparten el motor de vuelta, la telemetría y el dashboard, y la evolución completa queda almacenada junto con el resultado.

## Criterios cubiertos

| Criterio | Implementación |
|---|---|
| Variables climáticas | Temperatura ambiental, humedad, probabilidad e intensidad de lluvia, temperatura de pista y estado de pista. |
| Estados | Seco, nublado, lluvia ligera, lluvia y lluvia intensa. |
| Comportamiento progresivo | Las transiciones se calculan según el clima inicial y el riesgo histórico del circuito, sin saltos bruscos entre segmentos. |
| Rendimiento | Grip, tracción, frenado y temperatura de pista forman parte del factor de tiempo. |
| Piloto | La habilidad en lluvia gana peso cuando el estado deja de ser seco. |
| Neumáticos | Cambian temperatura, desgaste y recomendación entre slicks, intermedios y lluvia extrema. |
| Estrategia | El panel cambia entre ataque, equilibrio, conservación y máxima precaución. |
| Clasificación | Los tiempos, consumo y desgaste finales se calculan integrando las 20 condiciones, no únicamente mostrando mensajes. |

## Diseño y calidad

- `WeatherSnapshot` es inmutable, usa unidades explícitas y rechaza valores fuera de rango.
- `DynamicWeatherService` se ocupa solamente de generar una evolución suave y admite un generador aleatorio con semilla para pruebas reproducibles.
- `LapTimeCalculator` conserva el cálculo climático estático y añade sobrecargas para clima dinámico.
- La misma muestra modifica el motor, `SimulationSnapshot`, `TelemetrySnapshot` y la gráfica JavaFX; no existen estados visuales desconectados del resultado.
- El cálculo se ejecuta dentro del `Task` existente y JavaFX recibe únicamente actualizaciones mediante `Platform.runLater`.
- La evolución se serializa con la sesión para poder persistirla en MongoDB usando el Repository actual.

## Verificación

- 54 pruebas ejecutadas correctamente.
- Se verifican límites, suavidad, estados, efecto de lluvia sobre el tiempo, sincronización con telemetría, persistencia JSON y carga del FXML.
- La compilación con `-Xlint:unchecked` no produce advertencias.
