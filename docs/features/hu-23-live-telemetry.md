# HU-23 — Telemetría en vivo

## Resultado

La pantalla de simulación incorpora un dashboard de telemetría que se actualiza durante la vuelta del piloto seleccionado. La implementación cubre también los datos detallados descritos en HU-31, sin iniciar un motor paralelo ni bloquear JavaFX.

## Criterios cubiertos

| Dato solicitado | Implementación |
|---|---|
| Velocidad | Lectura en km/h limitada por la velocidad máxima del vehículo. |
| RPM | Valor derivado de la velocidad relativa y limitado a 15.000 rpm. |
| Combustible | Porcentaje restante de la carga asignada a la vuelta. |
| Desgaste de neumáticos | Acumulado de la vuelta, limitado al rango de 0 a 100 %. |
| Temperaturas | Neumáticos y motor reaccionan a clima, conducción, presión y ritmo. |
| Sector actual | La vuelta se divide de forma estable en tres sectores. |
| Tiempo de vuelta | Tiempo acumulado de la misma vuelta que genera la clasificación. |
| Delta | Diferencia acumulada frente al récord del circuito; verde si mejora y rojo si pierde tiempo. |
| Estado de pista | Estado legible derivado del clima de la sesión. |

## Diseño y calidad

- `TelemetrySnapshot` es un contrato inmutable con validación de texto, segmentos, porcentajes, temperaturas, RPM y valores finitos.
- La telemetría y la evolución de HU-19 nacen de las mismas 20 muestras; no pueden divergir ni duplicar el tiempo de ejecución.
- El cálculo ocurre dentro del `Task` de simulación. El controlador usa `Platform.runLater` únicamente para representar la lectura en controles JavaFX.
- La capa de servicio calcula los datos y el controlador se limita al formato visual. No se introdujeron patrones adicionales a Repository y Singleton.
- Los valores son telemetría simulada, no datos de sensores ni de OpenF1. El combustible representa la carga planificada para la vuelta y el delta usa el récord almacenado del circuito.

## Verificación

- La suite completa ejecuta 47 pruebas correctamente.
- La prueba del servicio comprueba 20 muestras, sincronía con HU-19, recorrido por tres sectores, convergencia del tiempo y monotonía de combustible, desgaste y cronómetro.
- Las pruebas del modelo rechazan lecturas fuera del contrato.
- `ViewsLoadTest` valida la carga de la vista FXML con todos sus controles nuevos.
