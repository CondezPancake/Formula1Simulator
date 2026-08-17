# HU-34 — Evolución del grip

## Resultado

HU-34 queda implementada como una evolución acumulativa de la pista entre las vueltas de la clasificación. El clima continúa perteneciendo a HU-29; HU-34 añade el efecto de la goma depositada por los monoplazas y su pérdida cuando llueve.

En seco, cada vuelta deposita goma progresivamente hasta alcanzar el límite de la pista. Esa goma recupera parte del grip, la tracción y la capacidad de frenado que faltan al inicio de una sesión con pista verde. Cuando aparece lluvia, el agua elimina goma en proporción a su intensidad y reduce nuevamente la adherencia.

## Integración con el motor

`TrackEvolutionService` es un servicio sin estado mutable compartido. Recibe:

- las 20 muestras climáticas de la vuelta;
- la goma acumulada al terminar la vuelta anterior;
- el número de vuelta y el piloto.

El resultado contiene las 20 condiciones de pista ajustadas, la goma final y un `TrackEvolutionSnapshot`. La clasificación utiliza esas condiciones antes de calcular:

- tiempo de vuelta;
- consumo y desgaste;
- parciales S1, S2 y S3;
- contexto de eventos;
- velocidad y telemetría.

Por tanto, la evolución no es una animación decorativa: modifica el resultado deportivo mediante `WeatherSnapshot.factorTiempo()`.

## Modelo y seguridad

- `TrackEvolutionSnapshot` es inmutable y valida vuelta, piloto, grip, goma y lluvia.
- Una lectura consolidada se guarda por piloto/vuelta dentro de `QualifyingSession`.
- El getter devuelve una copia inmutable.
- El servicio recibe el estado anterior y devuelve el siguiente; no conserva variables entre sesiones y puede usarse desde tareas concurrentes.
- La pista verde, el depósito máximo y el lavado por lluvia están acotados para evitar valores físicos inválidos.

## Interfaz

La pestaña **Evolución de pista** presenta:

- grip inicial y final de la sesión;
- cambio total de adherencia;
- gráfica de grip final y goma acumulada por vuelta;
- tabla con piloto, grip inicial/final, goma, lluvia promedio y tendencia.

La vista se encapsula en `TrackEvolutionController` y `track-evolution.fxml`, siguiendo el mismo patrón de componentes utilizado por HU-32 y HU-33.

## Persistencia y compatibilidad

- La evolución se serializa junto con la sesión y puede persistirse en MongoDB.
- Las sesiones creadas antes de HU-34, sin `evolucionPista`, continúan cargando y exponen una lista vacía.
- HU-29 mantiene las muestras detalladas de clima de la vuelta seleccionada; HU-34 conserva el resumen longitudinal de toda la sesión.

## Criterios de aceptación

| Criterio | Evidencia |
|---|---|
| Grip progresivo | Las vueltas secas aumentan goma y adherencia hasta un máximo. |
| Más goma en pista | Cada lectura conserva el nivel inicial y final de goma. |
| Lluvia | El agua elimina goma proporcionalmente a su intensidad. |
| Impacto deportivo | El grip ajustado participa en el factor real del tiempo de vuelta. |
| Evolución completa | Se almacena una muestra por piloto/vuelta. |
| Visualización | Gráfica, resumen y tabla independiente en JavaFX. |
| Persistencia | Ciclo JSON probado y compatible con sesiones antiguas. |

## Verificación

- Compilación de producción y pruebas con Java 17: correcta y sin advertencias.
- JUnit: **88 pruebas, 88 correctas, 0 fallos**.
- La suite cubre depósito en seco, límite de goma, lavado por lluvia, mejora del tiempo, invariantes, integración de 20 vueltas, inmutabilidad, persistencia histórica y carga real de los trece FXML.
