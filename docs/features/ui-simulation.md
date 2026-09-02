# feature/ui-simulation

## Por qué

Era la pantalla que da sentido al proyecto y estaba vacía: un botón sin comportamiento y una tabla sin columnas. También faltaba el historial completo.

## Vista de clasificación

Selección de circuito y vehículo, los cuatro ajustes de configuración (conducción, aerodinámica, presión y combustible), botón de inicio, `ProgressBar`, estado, clima generado y la parrilla final con la pole destacada.

**La configuración se guarda automáticamente**, como pide la HU: viaja dentro de la sesión que se persiste, y al abrir la pantalla se recuperan los valores de la última sesión disputada. No hay botón de guardar porque la rúbrica pide que sea automático.

## Concurrencia

Las propiedades de la interfaz se **enlazan** al `Task` en vez de asignarse:

```java
progreso.progressProperty().bind(tarea.progressProperty());
lblEstado.textProperty().bind(tarea.messageProperty());
btnSimular.disableProperty().bind(tarea.runningProperty());
```

Así la barra avanza, el texto va nombrando a cada piloto y el botón se deshabilita solo mientras la sesión corre, sin un solo `Platform.runLater`. Los enlaces se deshacen en `setOnSucceeded`/`setOnFailed` para poder volver a escribir en esos controles. El guardado posterior también va en segundo plano: la parrilla ya está en pantalla y no debe esperar a la base de datos.

## Un ajuste de realismo

La primera prueba end-to-end destapó un problema de diseño: había **5,7 segundos** entre el coche del usuario y el resto de la parrilla. La causa era que el usuario elegía modo agresivo mientras los rivales corrían con la configuración neutra.

En una clasificación real todos los equipos aprietan, así que ahora los rivales usan `SimulationConfig.paraClasificacion()` (modo agresivo). El resultado pasa a ser una parrilla creíble:

```
 1. Max Verstappen    Red Bull      1:16.453      —
 2. Sergio Pérez      Red Bull      1:16.576  +0.124
 3. Carlos Sainz      Ferrari       1:16.711  +0.259
...
20. Logan Sargeant    Williams      1:20.859  +4.407
```

4,4 s de la pole al colista sobre una vuelta de ~77 s (un 5,7 %), compañeros de equipo agrupados y los equipos punteros delante. Con la configuración anterior el usuario ganaba por goleada solo por elegir el modo agresivo.

## Historial

Tabla de sesiones (fecha, circuito, clima, pole y configuración empleada), parrilla completa de la sesión seleccionada, `LineChart` que compara el tiempo de pole entre sesiones del mismo circuito, y reutilización de configuraciones previas. Cubre las tres HU de almacenamiento.

## Mantenibilidad y actualización incremental

`TelemetryDetailPresenter` es responsable de agrupar vueltas, gestionar el
selector y mantener las siete series del detalle. `SimulationController` solo
le entrega sesiones completas o nuevas muestras, por lo que ya no contiene el
algoritmo de construcción de gráficas.

Durante una sesión cada muestra se añade a las series existentes. Antes se
reconstruían todas las vueltas y todos sus puntos en cada segmento. La torre de
clasificación y la tabla de eventos también conservan una única
`ObservableList` y actualizan su contenido con `setAll`, evitando sustituir el
modelo visual y perder estado de selección en cada fotograma.

## Verificación

- `mvn clean test` → **40 tests, 0 fallos**, incluida la carga real de `simulation.fxml` e `history.fxml`.
- Prueba end-to-end ejecutada contra MongoDB: se simula una sesión completa, se ordenan los 20 pilotos, se persiste y el historial pasa de 1 a 2 entradas con su configuración guardada.
