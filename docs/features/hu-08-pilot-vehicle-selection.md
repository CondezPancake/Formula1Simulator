# HU-08 — Seleccionar piloto y vehículo

## Objetivo

Permitir que el usuario elija un piloto y el vehículo que conduce antes de iniciar la clasificación, evitando combinaciones inválidas entre pilotos y escuderías.

## Criterios implementados

- La pantalla de simulación permite seleccionar un circuito, un vehículo y un piloto.
- Al cambiar el vehículo, el selector muestra únicamente sus pilotos asignados.
- No se puede iniciar una simulación sin completar las tres selecciones.
- El servicio comprueba nuevamente que el piloto y el vehículo existan y que estén relacionados.
- Los ajustes del usuario se aplican únicamente al piloto seleccionado. Su compañero y el resto de la parrilla conservan la configuración general de clasificación.
- El identificador del piloto queda guardado dentro de `SimulationConfig` y se recupera con la última configuración.
- Las sesiones antiguas que no contienen `pilotoId` siguen siendo legibles; la interfaz selecciona el primer piloto válido del vehículo.

## Decisiones de diseño

La relación se valida en dos niveles:

1. El controlador filtra las opciones para ofrecer una interacción clara.
2. `QualifyingService` protege la regla de negocio para que una llamada directa, una prueba o una futura interfaz tampoco pueda simular una combinación inválida.

Se persiste el identificador del piloto y no una copia completa del objeto. Esto evita duplicar información y mantiene `SimulationConfig` como una referencia estable a la entidad administrada por la capa de datos.

## Verificación

La clase `QualifyingServiceTest` ejecuta **12 pruebas correctas**, incluidas las que cubren que:

- La configuración personalizada afecta únicamente al piloto elegido.
- El compañero de equipo mantiene su propia configuración de clasificación.
- Una combinación piloto/vehículo inválida produce una `ValidationException` comprensible.
- El piloto seleccionado permanece dentro de la configuración de la sesión.

La suite completa encuentra dos fallos anteriores a HU-08 en `FormatUtilsTest`: dependen del separador decimal regional y esperan punto aunque el entorno colombiano produce coma. No afectan la selección ni la simulación de esta historia.
