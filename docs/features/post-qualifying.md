# HU-53 — Resultados posteriores a la clasificación

## Flujo

Cuando la tarea de simulación termina, `SimulationController` carga la sesión
en `PostQualifyingController`, habilita la pestaña **Resultados** y navega hacia
ella. La simulación y el guardado no dependen de esta vista: el componente es un
lector de `QualifyingSession`.

## Información presentada

- clasificación final, tiempo, gap y los tres sectores;
- compuesto final, número de paradas y eventos por piloto;
- piloto fijado durante la sesión o, como alternativa, el piloto configurado;
- vehículo, equipo, estado de vuelta y configuración utilizada;
- compuesto inicial y final, cambios, desgaste y pérdida en boxes;
- radio derivada de eventos, decisiones de boxes y cambios de neumáticos;
- circuito, clima, fecha, pole, participantes, vueltas válidas y tiempo medio.

La selección de una fila actualiza únicamente el informe lateral. No modifica
los resultados ni vuelve a ejecutar cálculos deportivos.

La tabla adopta el lenguaje visual de clasificación: franja del color de la
escudería, pole amarilla, podio diferenciado, vueltas inválidas atenuadas,
compuestos S/M/H con su color y métricas de boxes/eventos resaltadas. La fila
del piloto configurado conserva un fondo azul y la selección activa usa el rojo
de la aplicación. La antigua pestaña **Clasificación** se retiró porque esta
sección conserva sus datos y añade el informe completo sin duplicarlos.

## Responsabilidades

- `PostQualifyingController`: transforma una sesión terminada en información
  visual y gestiona la selección de la tabla.
- `post-qualifying.fxml`: define exclusivamente la composición del informe.
- `SimulationController`: entrega la sesión y realiza la transición final.
- `QualifyingSession`: continúa siendo la fuente persistible de los datos.

Las sesiones antiguas siguen siendo compatibles: sin estrategia de neumáticos
se muestra Medium y, si faltan incidencias, el informe indica una vuelta limpia.

## Verificación

Se revisaron estáticamente las referencias FXML, los campos persistidos y el
diff. No se ejecutaron pruebas ni compilación por indicación del usuario.
