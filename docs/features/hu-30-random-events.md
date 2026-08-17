# HU-30 — Sistema de eventos aleatorios

## Resultado

HU-30 queda implementada con 28 eventos reales más `NO_EVENT`. El sistema decide primero la categoría y después selecciona por peso solamente entre los eventos compatibles con el estado de la vuelta. Los eventos no se limitan a mensajes: modifican tiempo, velocidad, grip, desgaste, temperaturas, clima, validez de la vuelta y estado del piloto.

La configuración normal es:

| Categoría | Probabilidad base |
|---|---:|
| Sin evento | 72 % |
| Positivo | 8 % |
| Negativo leve | 12 % |
| Negativo importante | 5 % |
| Clima/pista | 2 % |
| Excepcional | 1 % |

Estas probabilidades se encapsulan en `EventProbabilityConfig`. Piloto, estrategia, clima, grip, temperatura, desgaste, tráfico y vehículo ajustan el riesgo contextual sin evaluar cada evento como una tirada independiente.

## Diseño

- `SimulationEvent` define el contrato polimórfico de compatibilidad, peso e impacto.
- `EventCatalog` compone el catálogo sin un `switch` gigante.
- `WeightedEventSelector` implementa la ruleta ponderada reutilizable.
- `EventManager` controla las dos etapas, el cooldown, la semilla y el límite de eventos.
- `EventContext` concentra el estado de solo lectura de la vuelta.
- `EventEffectService` aplica impactos al clima, resultado y telemetría.
- `EventOccurrence` y `EventImpact` son valores inmutables y persistibles.

El catálogo usa especializaciones para eventos de rendimiento, pista y accidente. Normalmente se produce cero o un evento; existe una probabilidad configurable del 0,5 % de sumar un evento del alcance opuesto, siempre que sea compatible. Nunca hay más de un evento individual ni más de uno global por resolución.

## Accidente

`Crash / Accident` pertenece a la categoría excepcional y tiene un peso menor que `Red Flag`. Su riesgo aumenta con lluvia, poco grip, desgaste, estrategia agresiva, temperatura/vehículo y errores previos, y disminuye con habilidad y consistencia.

Al ocurrir:

- no se registra tiempo válido;
- se conserva el sector del impacto;
- la telemetría queda en velocidad cero desde ese sector;
- el resultado pasa a `INVALID` o `OUT` según gravedad;
- activa `YELLOW` o `RED`;
- consumo y desgaste reflejan la fracción de vuelta completada;
- el incidente se persiste con la sesión y se muestra en clasificación, telemetría y la pestaña Eventos.

## Criterios de aceptación

| Criterio | Evidencia |
|---|---|
| 20 eventos + `NO_EVENT` | 28 eventos distintos + `NO_EVENT`; una prueba verifica cobertura exacta y ausencia de duplicados. |
| Mayoría sin eventos | 72 % base; una prueba estadística de 30.000 resoluciones exige que `NO_EVENT` sea claramente mayoritario. |
| Selección ponderada | Selección en dos etapas y prueba de una distribución 9:1. |
| Compatibilidad contextual | Reglas probadas para tráfico, temperaturas, lluvia, secado y grip. |
| Impactos reales y variables | Los resultados, el clima y la telemetría consumen `EventImpact`; rangos aleatorios acotados. |
| Piloto, estrategia y vehículo | Multiplicadores separados en `EventContext`; prueba comparativa ATTACK/baja consistencia frente a CONSERVE/alta consistencia. |
| Límite y cooldown | Máximo 1 individual + 1 global compatible; prueba de bloqueo y reactivación tras cooldown. |
| Sectores | Todos los eventos ocurridos usan `SECTOR_1`, `SECTOR_2` o `SECTOR_3`. |
| Semilla | Constructores con `new Random()` o `new Random(seed)`; dos managers con igual seed producen impactos idénticos. |
| Accidente excepcional | Prueba estadística exige frecuencia menor al 1 % en condiciones normales. |
| Accidente integrado | Prueba de sesión fuerza accidentes y verifica resultados, telemetría, sector, invalidez y ausencia de pole. |
| Compatibilidad regresiva | 68 pruebas pasan, incluida la carga de las diez vistas FXML, el round-trip JSON de un accidente y 250 carreras consecutivas. |

## Verificación

- Compilación de producción con Java 17: correcta.
- Compilación de pruebas: correcta.
- JUnit: **68 pruebas, 68 correctas, 0 fallos**.
