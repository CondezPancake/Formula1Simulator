# feature/qualifying-engine

## Por qué

El motor de simulación era la pieza que daba nombre al proyecto y **no existía**: `LapCalculator`, `PerformanceCalculator` y `SimulationEngine` lanzaban `UnsupportedOperationException`. Esta rama lo implementa en 2 clases en vez de 4.

## Fórmula

```
t_base   = 3600 · longitud_km / velocidad_promedio(modo)
t_vuelta = t_base · factorTecnico · f_clima · f_aero · f_presion
                  · f_combustible · f_piloto · (1 + ε)
```

Cada factor vive como campo del enum correspondiente, así que la fórmula no necesita tablas externas ni un solo `switch`.

**`factorTecnico` no es un número inventado**: cada circuito lo deriva de su récord de vuelta real (Mónaco 1,98 · Monza 1,32). Que Mónaco sea lento y Monza rápido sale de los datos de la especificación.

`f_piloto = 1 − 0,040·(habilidad/100) − 0,015·min(exp,10)/10`, donde la habilidad pondera velocidad y consistencia en seco, y añade la habilidad con lluvia cuando el piso está mojado. Un piloto de élite y veterano rebaja el tiempo hasta un 5,5 %.

`ε` es una variación acotada de ±0,5 %: suficiente para que dos compañeros de equipo se intercambien posiciones, insuficiente para que un Williams haga la pole.

### Contrapartidas

Ninguna configuración es gratis, para que elegir importe:

| Ajuste | Tiempo | Consumo | Desgaste |
|---|---|---|---|
| Aero baja / alta | +1,0 % / −0,5 % | ×0,95 / ×1,08 | — / ×1,05 |
| Presión baja / alta | −0,5 % / +0,5 % | — | ×1,15 / ×0,90 |
| Combustible agresivo / ahorro | −1,0 % / +1,0 % | ×1,15 / ×0,85 | — |

El consumo y el desgaste por vuelta salen de `rendimiento[modo]` cruzado con el clima y los factores del circuito, que es justo lo que pide la HU «impacto del circuito en desgaste y consumo».

## Concurrencia

- `util/Async`: un único pool de 2 hilos **demonio** compartido. Que sean demonio evita que la aplicación se quede colgada al cerrarse, y que sea único evita que cada controlador cree el suyo.
- `QualifyingService.crearTarea(config)` devuelve un `Task<QualifyingSession>` con `updateProgress`/`updateMessage`. El `Task` ya publica esos valores en el hilo de JavaFX, así que **no hace falta `Platform.runLater`** dentro de él.
- `LapTimeCalculator` no tiene estado mutable: es seguro llamarlo desde cualquier hilo.
- `RITMO_MS = 80` por piloto (1,6 s en total). Es deliberado: sin esa pausa la sesión terminaría en microsegundos y no se podría *demostrar* que la interfaz sigue respondiendo mientras el cálculo corre aparte.

## Diseño

`LapTimeCalculator` acepta un `Random` en el constructor. Con semilla fija el resultado es determinista, que es lo que permite testear el motor de verdad en lugar de solo comprobar que no lanza.

Solo el vehículo elegido por el usuario hereda su configuración; el resto de la parrilla corre con la neutra. Así los ajustes se notan en la clasificación en vez de anularse.

## Verificación

`mvn clean test` → **36 tests, 0 fallos**. Los que de verdad validan el motor:

- **Cordura de escala**: el tiempo de Verstappen con el RB20 en Monza cae a menos de 3 s del récord real (1:21.046). Es la prueba de que la fórmula produce tiempos de Fórmula 1 y no números arbitrarios.
- **Monotonía**: agresiva < normal < ahorro, y seco < lluvioso < extremo.
- **Determinismo**: misma semilla, mismo tiempo.
- **Mónaco vs Monza**: aunque la vuelta de Mónaco es más corta en tiempo (es un circuito más corto), su tiempo *por kilómetro* es un 40 % peor.
- **Parrilla**: 20 pilotos, posiciones 1..20 consecutivas, ordenadas por tiempo, gap de la pole = 0, y **dispersión pole-colista entre 1 y 10 s** — realista, ni un empate ni un minuto.
- **Clima**: sobre 200 tiradas en Yas Marina (95 % seco) salen mayoría de sesiones en seco, así que la distribución por circuito se respeta.
- **Contrapartidas**: ahorro consume menos que empujar, y menos presión desgasta más.
