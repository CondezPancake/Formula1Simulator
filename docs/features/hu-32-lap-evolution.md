# HU-32 — Visualizar evolución de la vuelta

## Resultado

HU-32 queda implementada mediante una nueva pestaña **Evolución de vuelta**. La vista representa las 20 muestras reales producidas por el motor para el piloto configurado; no genera valores decorativos ni vuelve a calcular la simulación desde JavaFX.

El usuario puede visualizar por segmento:

- velocidad;
- tiempo acumulado;
- desgaste de neumáticos;
- combustible restante;
- temperatura de neumáticos;
- temperatura del motor;
- delta frente al récord.

## Experiencia de usuario

- Un `LineChart` se actualiza durante la vuelta y permite cambiar de métrica sin repetir la carrera.
- Un `TableView` conserva el detalle de los 20 segmentos: sector, velocidad, tiempo, delta, combustible, desgaste, temperaturas y estado.
- El resumen indica progreso, sector, rango de la métrica y si el piloto gana o pierde tiempo frente al récord.
- Una vuelta invalidada o un accidente conserva `INVALID/OUT` y velocidad cero desde el incidente, por lo que HU-32 permanece coherente con HU-30.

## Arquitectura

`TelemetrySnapshot` continúa siendo la fuente única de las métricas. `QualifyingService` captura siempre las muestras del piloto seleccionado y las incorpora a `QualifyingSession.evolucionVuelta`, aunque la simulación se ejecute sin callbacks visuales.

La interfaz está encapsulada en `LapEvolutionController` y su FXML independiente. El controlador principal solamente coordina el inicio y cierre de la sesión; el componente de HU-32:

1. recibe la muestra inmutable;
2. la incorpora al gráfico y la tabla;
3. selecciona mediante una función qué propiedad representar.

El selector utiliza referencias a métodos en lugar de un bloque condicional por métrica. También se reemplaza una muestra si JavaFX entrega nuevamente el mismo segmento, evitando duplicados por sincronización entre el flujo en vivo y el cierre del `Task`.

## Persistencia y compatibilidad

- Las 20 muestras se serializan con la sesión y quedan preparadas para consulta histórica.
- El getter devuelve una copia inmutable para no exponer el estado interno.
- Las sesiones antiguas que no contienen `evolucionVuelta` siguen abriendo correctamente y devuelven una lista vacía.
- Eventos, clima y estado de vuelta permanecen dentro de cada muestra serializada.

## Criterios de aceptación

| Criterio | Evidencia |
|---|---|
| Visualizar rendimiento durante la vuelta | Gráfico actualizado con las 20 muestras conforme avanza la simulación. |
| Velocidad | Métrica en km/h disponible en gráfico y tabla. |
| Tiempo | Tiempo acumulado disponible en segundos y con formato de vuelta. |
| Desgaste | Porcentaje de desgaste disponible en gráfico y tabla. |
| Combustible | Porcentaje restante disponible en gráfico y tabla. |
| Temperatura | Temperaturas de neumáticos y motor disponibles por separado en el gráfico. |
| Delta | Evolución frente al récord y resumen de ganancia/pérdida. |
| Identificar dónde cambia el rendimiento | Eje por segmento, sector visible y tabla detallada. |
| Datos conectados al motor | Se reutiliza exactamente el `TelemetrySnapshot` emitido y persistido. |
| No romper HU-30/HU-31 | Accidentes, eventos, estado y clima continúan reflejándose en las muestras. |

## Verificación

- Compilación de producción con Java 17: correcta.
- Compilación de pruebas: correcta.
- JUnit: **71 pruebas, 71 correctas, 0 fallos**.
- Incluye carga real de los once FXML, persistencia JSON, compatibilidad con sesiones antiguas y 250 carreras consecutivas.
