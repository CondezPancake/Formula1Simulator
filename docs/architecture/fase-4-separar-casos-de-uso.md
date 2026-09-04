# Fase 4 — separar casos de uso y dominio

## Punto de partida

`QualifyingService.simular(...)` ya era, antes de esta fase, un método sin
ninguna dependencia de JavaFX en su firma ni en su cuerpo: usa únicamente sus
propias interfaces (`Progreso`, `Evolucion`, `Telemetria`,
`EvolucionPista`, `ClasificacionEnVivo`, `EventosEnVivo`, `PitStopsEnVivo`,
`CambiosNeumaticosEnVivo`, `ControlSimulacion`), no tipos de `javafx.*`. El
acoplamiento a JavaFX que señalaba el diagnóstico vivía en un lugar preciso y
acotado: los métodos `crearTarea(...)` (10 sobrecargas) que envolvían
`simular(...)` en un `javafx.concurrent.Task`.

## Cambio

Se extrajeron los 10 `crearTarea(...)` y el `Task` anónimo, sin modificar una
sola línea de su lógica, a una clase nueva:
**`QualifyingSessionTaskFactory`**. `QualifyingService` ya no importa
`javafx.concurrent.*` — verificado (`grep javafx` sobre el archivo no
devuelve nada).

Para que la fábrica nueva pudiera seguir llamando a las piezas que ya usaba
(`simular(...)` de 11 parámetros, `validarSeleccion(...)`, la interfaz
`ControlSimulacion`, la constante `SEGMENTOS_EVOLUCION`, la clase
`SimulationPacer`), solo hizo falta ensanchar una visibilidad:
`validarSeleccion` pasó de `private` a paquete — el resto ya era paquete y
`QualifyingSessionTaskFactory` vive en el mismo paquete `service`. Ninguna
otra firma cambió.

**Por qué se queda en `service` y no en `controller` todavía**: moverla a
`controller` habría exigido ensanchar `ControlSimulacion`, `simular(...)` y
`SEGMENTOS_EVOLUCION` a `public`, ampliando la superficie pública de
`QualifyingService` solo para relocalizar un archivo — eso es exactamente el
trabajo de la Fase 7 ("mover paquetes gradualmente"), no de esta. Aquí el
objetivo es que el caso de uso deje de *contener* la construcción del
`Task`, no que el archivo cambie de carpeta.

## Único punto de llamada

`crearTarea(...)` solo lo invocaba `SimulationController` (un sitio, con la
sobrecarga completa de 9 parámetros). Se actualizó ese único punto de llamada
en el mismo cambio — no se dejó una fachada de compatibilidad en
`QualifyingService` porque, al no haber más consumidores, mantenerla habría
significado que `service` volviera a depender de `controller` para
delegar, invirtiendo la dirección de dependencia que pide la arquitectura
hexagonal. Con un solo llamador y la Fase 5 autorizada en la misma sesión,
extracción y actualización del llamador se hicieron como un único cambio
verificable en vez de dos pasos con un intermedio que no compila.

## Verificación

- `grep javafx` sobre `QualifyingService.java`: sin resultados.
- `mvn clean test`: 172/0/0, 4 *skipped* (MySQL) — misma cuenta que en Fase 3,
  cero regresiones.
- `QualifyingServiceTest` (24 tests, varios con semillas de eventos fijas)
  sigue en verde: como el cuerpo de `simular(...)` y sus métodos privados
  auxiliares no se tocaron —solo se movió el código que lo envolvía en un
  `Task`—, la clasificación, los eventos y los snapshots son
  indistinguibles antes y después por construcción, no solo por test.

## Veredicto

`QualifyingService` y las reglas de simulación se pueden ejecutar sin
inicializar JavaFX. Sigue dependiendo de `QualifyingDataPort` (Fase 2), no de
JDBC ni del singleton `DataStore` directamente (salvo el constructor sin
argumentos, documentado como compatibilidad temporal con FXML).
