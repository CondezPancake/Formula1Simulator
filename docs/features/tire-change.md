# HU-51 — Cambio de neumáticos

## Compuestos

El dominio incorpora los tres compuestos solicitados:

| Compuesto | Código | Factor de tiempo | Factor de desgaste |
|---|---|---:|---:|
| Soft | S | 0,985 | 1,28 |
| Medium | M | 1,000 | 1,00 |
| Hard | H | 1,012 | 0,76 |

Soft ofrece más ritmo y se degrada antes; Hard conserva el neumático a cambio
de tiempo; Medium es el punto inicial equilibrado. Los factores solo afectan
el tramo recorrido después del cambio, no reescriben el rendimiento previo.

## Flujo automático

El usuario elige S, M o H como compuesto inicial de su piloto desde
**Configuración**; Medium sigue siendo el valor predeterminado y el inicial de
los rivales. Cuando `PitStopService` alcanza la fase `STOPPED`,
`TireStrategyService` solicita a `TireCompoundPolicy` el siguiente compuesto y
registra el cambio. No existe una acción manual de entrada a boxes en el Dashboard.

La política contextual usa el motivo de la parada:

- neumáticos o desgaste: monta una opción más durable;
- riesgo mecánico: busca recuperar ritmo;
- pista secándose: monta Soft;
- lluvia nueva o creciente: monta la opción más durable disponible.

El alcance vigente solo contempla S, M y H, como pide HU-51. No se inventaron
Intermedios o Wet; podrán añadirse ampliando el enum y la política cuando el
backlog los defina.

## Responsabilidades

- `TireCompound`: datos y contrapartidas de cada compuesto.
- `TireChangeRecord`: cambio inmutable y persistible, enlazado por id al pit stop.
- `TireCompoundPolicy`: contrato para elegir el siguiente neumático.
- `ContextualTireCompoundPolicy`: decisión automática actual.
- `TireStrategyService`: estado por piloto, transiciones, historial y efectos.
- `TireChangePresenter`: compuesto del coche configurado, mensajes y feed.
- `QualifyingService`: coordina el cambio cuando la parada está detenida.

El método de cambio acepta cualquier transición entre compuestos distintos;
la política no está incrustada en el servicio, por lo que puede sustituirse sin
modificar la ejecución o la persistencia.

## Dashboard y persistencia

Se añadió la lectura **COMPUESTO** junto a las lecturas existentes de neumático
y combustible: S roja, M amarilla y H blanca. No se retiró ni reorganizó ningún
control. Todos los cambios aparecen además en el feed con piloto, transición y
segmento.

`SimulationConfig.compuestoInicial` conserva la elección del piloto y
`QualifyingSession.cambiosNeumaticos` conserva el historial. Las sesiones
anteriores, sin esos campos, se interpretan como Medium y una lista vacía.

## Verificación

Se revisaron estáticamente contratos, callbacks, FXML, persistencia y el diff.
No se ejecutaron pruebas ni compilación por indicación del usuario.
