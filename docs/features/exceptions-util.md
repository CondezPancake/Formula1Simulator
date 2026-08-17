# Utilidades y excepciones

## Qué contiene hoy

El proyecto conserva una capa pequeña de utilidades transversales y dos excepciones de aplicación:

- `service.ValidationException`: errores de reglas de negocio o selección inválida antes de simular.
- `data.DataAccessException`: errores de lectura/escritura persistente.
- `util.DateUtils`: fecha de sesión.
- `util.FormatUtils`: tiempos, gaps, deltas y porcentajes.
- `util.MathUtils`: límites y cálculos numéricos simples.
- `util.RandomUtils`: aleatoriedad acotada.
- `util.ValidationUtils`: validaciones de texto, rangos y números positivos.
- `util.Async`: pool compartido para tareas fuera del hilo JavaFX.

## Decisiones

Las excepciones son `RuntimeException` porque la UI y los servicios las manejan en los bordes de cada flujo. Esto evita propagar `throws` por todo el dominio y mantiene el código de simulación enfocado en reglas.

Las utilidades son clases finales o de uso estático con responsabilidad acotada; no contienen lógica de negocio de Fórmula 1.

## Verificación

La suite completa ejecuta 94 pruebas correctamente.
