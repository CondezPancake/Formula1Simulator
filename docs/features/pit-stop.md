# HU-50 — Parada en boxes

## Comportamiento

Durante una sesión en vivo el motor evalúa automáticamente a todos los pilotos.
Cuando el contexto exige una intervención, genera una decisión de boxes que
recorre cuatro estados observables: entrada, detención, salida y parada
completada. El usuario observa la estrategia, pero no ordena la parada.

La política actual entra a boxes por una de estas razones:

- sobrecalentamiento o temperatura insuficiente de neumáticos;
- sobrecalentamiento de frenos o motor, pérdida de potencia o problema
  mecánico;
- inicio, intensificación o final de lluvia y secado de la pista;
- desgaste acumulado igual o superior al 12 %.

La pérdida se incorpora progresivamente al tiempo acumulado. Como la sesión
abreviada representa una sola vuelta y no una carrera completa, el tránsito de
entrada y salida se escala a 0,45 s y 0,55 s; la detención dura entre 1,8 s y
2,8 s. Así el coste total queda entre 2,8 s y 3,8 s, en vez de trasladar los
17–19 s de un paso completo por el pit lane a una única vuelta simulada. Por tanto, la
misma lista que alimenta la torre y el mapa se vuelve a ordenar y muestra la
posición real después de la parada. Al terminar, el tiempo perdido también se
aplica al sector donde comenzó la entrada, de modo que tiempo total y parciales
continúan siendo consistentes.

La parada solo se acepta si la vuelta sigue activa y quedan segmentos para
completar todas sus fases. En esta clasificación de una vuelta se permite una
por piloto. Un accidente o una vuelta ya invalidada no genera una parada.

## Responsabilidades

- `PitStopDecision`: decisión estructurada con piloto y motivo.
- `PitStopPolicy`: contrato de decisión sustituible.
- `ContextualPitStopPolicy`: reglas basadas en el estado de la simulación.
- `PitStopPhase`: vocabulario estable de estados.
- `PitStopRecord`: snapshot inmutable y persistible; no contiene decisiones de
  interfaz, neumáticos ni radio.
- `PitStopService`: acepta órdenes, avanza fases y calcula la pérdida deportiva.
- `QualifyingService`: coordina el servicio con el reloj y reordena la parrilla.
- `SimulationController`: recibe los cambios de fase y coordina su presentación.
- `PitStopPresenter`: representa fases, radio y feed sin contaminar el
  controlador con formato visual.
- `QualifyingSession`: conserva el último estado de cada parada para MongoDB y
  modo memoria.

`PitStopService` recibe su generador aleatorio por constructor y
`QualifyingService` admite inyectar tanto el servicio como la política. Esto
mantiene determinista el cálculo, evita ocultar dependencias dentro del flujo y
permite cambiar las reglas estratégicas sin modificar la ejecución del pit stop.

## Extensiones previstas

HU-51 asocia el cambio de neumáticos a la fase `STOPPED` sin cambiar la decisión
ni el ciclo de la parada. HU-49 podrá consumir los snapshots desde
el callback `PitStopsEnVivo` para construir una radio más rica; por ahora los
mensajes estructurados se reflejan en el panel existente de ingeniería.

## Persistencia y visualización

Cada sesión guarda `paradasBoxes` junto con resultados, eventos y telemetría.
Carrera muestra fase, tiempo detenido, pérdida y cambio de posición; el feed
conserva la incidencia y el historial de sesiones muestra el número de paradas.
Las sesiones antiguas, sin el campo nuevo, se leen como una lista vacía.

## Verificación

Se realizó revisión estática de imports, firmas, FXML, serialización y
`git diff --check`. No se ejecutaron pruebas ni compilación por indicación del
usuario.
