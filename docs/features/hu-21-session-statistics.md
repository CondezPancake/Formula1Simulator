# HU-21 — Visualizar estadísticas

## Objetivo

Permitir que el usuario compare gráficamente el rendimiento de los 20 participantes al finalizar una clasificación.

## Criterios implementados

- La pantalla separa la clasificación y las estadísticas mediante pestañas para conservar legibilidad.
- Un gráfico de barras representa a todos los participantes de la sesión actual.
- El usuario puede alternar entre tiempo de vuelta, diferencia con la pole, consumo y desgaste.
- El eje y el título del gráfico cambian junto con la métrica seleccionada.
- Se muestran pole, tiempo promedio, diferencia máxima y cantidad de participantes.
- El gráfico se limpia al comenzar una sesión nueva y solo presenta resultados de una simulación completada.

## Diseño

`SessionStatistics` es un `record` inmutable que contiene el resumen numérico y valida que sus métricas sean finitas y no negativas.

`QualifyingService.calcularEstadisticas` utiliza Streams y `DoubleSummaryStatistics` para calcular el resumen sin depender de JavaFX. `SimulationController` transforma los resultados en una serie gráfica y mantiene las decisiones de presentación dentro de la capa de interfaz.

La selección de métricas utiliza un enum con comportamiento específico por métrica. Esto evita condicionales repetidos y mantiene en un solo lugar las etiquetas, unidades y extracción de valores.

## Verificación

- Una prueba comprueba los 20 participantes, pole, promedio, diferencia máxima, consumo y desgaste.
- `ViewsLoadTest` carga la pestaña, el selector, los indicadores y el gráfico desde el FXML real.
- La suite completa ejecuta 43 pruebas correctamente.
