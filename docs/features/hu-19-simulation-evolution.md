# HU-19 — Mostrar evolución

## Objetivo

Mostrar durante la clasificación cómo evolucionan la velocidad, el consumo y el desgaste del vehículo configurado por el usuario.

## Criterios implementados

- La vuelta del piloto seleccionado se divide en 20 segmentos visibles.
- La evolución comienza antes de procesar al resto de participantes.
- Se muestran el piloto, el vehículo, el progreso de vuelta y la velocidad en km/h.
- El consumo y el desgaste se presentan como valores acumulados frente al total calculado.
- La velocidad parte del promedio efectivo de la vuelta y nunca supera la velocidad máxima del vehículo.
- Los valores finales de consumo y desgaste coinciden exactamente con los almacenados en `LapResult`.
- La simulación continúa en un hilo de trabajo y todas las actualizaciones visuales se despachan de forma segura al hilo de JavaFX.

## Diseño

`SimulationSnapshot` es un `record` inmutable que representa una muestra. Valida sus invariantes al construirse y ofrece valores normalizados para las barras de progreso.

`QualifyingService` produce las muestras mediante un callback independiente de la interfaz. El servicio no conoce controles JavaFX, mientras que `SimulationController` se limita a presentar cada muestra recibida.

El consumo y el desgaste no son animaciones decorativas: avanzan proporcionalmente hasta los totales producidos por `LapTimeCalculator`. La velocidad se deriva de la longitud y el tiempo de vuelta calculado, incorpora una variación controlada durante el recorrido y se limita por la velocidad máxima del monoplaza.

## Verificación

- La prueba de evolución comprueba las 20 muestras, su orden y acumulación creciente.
- La última muestra se compara con el resultado real de la sesión.
- Las pruebas cargan `simulation.fxml` para detectar identificadores o controles inválidos.
- La suite completa ejecuta 42 pruebas correctamente.
