# Radio del box (HU-49) y piloto fijado (HU-48)

## Por qué

El Dashboard ya contaba lo que pasaba —eventos, paradas, cambios de neumático—
pero lo contaba como un registro: filas de incidencias, todas con el mismo tono
y sin destinatario. En una sesión real esa información llega por radio, dirigida
a un piloto concreto, y eso es justo lo que faltaba para que la pantalla se
sintiera una carrera y no un panel de control.

La HU-48 es su condición previa: para que la radio tenga a quién escuchar, el
usuario tiene que poder **fijar** un piloto y que ese estado sobreviva a que el
ratón se vaya a curiosear otro.

## HU-48 — consultado y fijado son dos estados

El documento de historias lo dice explícitamente: *"Piloto seleccionado y piloto
fijado son estados distintos. El usuario puede consultar a un piloto mientras
mantiene a otro fijado"*. Antes eran el mismo, con dos consecuencias:

- El mapa llamaba a `resaltar(...)` desde su propio hover sin saber nada del
  fijado, así que **pasar el ratón por el trazado borraba la marca** del piloto
  que el usuario había fijado. Los rótulos de sector seguían hablando de él y el
  coche ya no estaba señalado: dos verdades distintas en la misma pantalla.
- `lblSeleccion` se escribía solo al pulsar, con la posición y el gap de ese
  instante. En cuanto el piloto cambiaba de posición, el rótulo mentía.

Ahora `CircuitoEnVivo` tiene **dos propiedades independientes**: `pilotoResaltado`
(tanteo del ratón, se va con él) y `pilotoFijado` (persiste hasta que el usuario
lo cambie). El fijado se pinta con un segundo anillo y con etiqueta permanente
que lleva su trigrama —seguir `VER` es más legible que seguir `P3`—, y el orden
de pintado pone al usuario por encima del fijado, y a este por encima del
consultado.

El fijado **no se limpia al terminar la sesión**: se vuelve a marcar sobre la
parrilla final y `PostQualifyingController` lo usa para destacar su resultado.

## HU-49 — la radio

### Modelo

`RadioMessage` es un record con emisor (ingeniero o piloto), texto, segmento,
sector y prioridad. La prioridad no es decoración: decide qué mensajes merecen
interrumpir con el rótulo sobre el mapa y un aviso sonoro, y cuáles se quedan en
el historial lateral.

### Generación

`RaceRadioService` traduce señales del motor a habla de muro de boxes. Está
calcado del registro real, que es telegráfico —instrucción, número y acuse— y en
el que el ingeniero solo abre el canal cuando tiene algo que decir. De ahí dos
decisiones:

- **Cada aviso se da una vez.** La telemetría llega veinte veces por sesión; un
  ingeniero que repitiera "los neumáticos están cayendo" en cada muestra sería
  ruido, así que el servicio recuerda lo que ya dijo.
- **La posición solo se canta cuando cambia.** El gap por sí solo no es noticia
  en una clasificación; ganar o perder un puesto sí.

Ninguna frase inventa datos: todas nacen de una señal que el motor ya produce
—posición, gap, sector, desgaste, combustible, temperaturas, clima, bandera,
evento, parada o cambio de compuesto—. El servicio no depende de JavaFX y se
prueba sin levantar el toolkit (`RaceRadioServiceTest`, 12 casos).

### Cadencia

El motor **no emite las incidencias goteando**: las suelta en tres ráfagas, al
cambiar de sector (segmentos 1, 8 y 15). Sin regular la salida, la radio
escupiría diez frases de golpe y no se leería ninguna. `RadioPresenter` mantiene
una cola que sirve un mensaje cada 900 ms, que es más o menos lo que dura una
frase real de radio.

### Presentación

Dos salidas para el mismo mensaje:

- **Hilo lateral**: la conversación completa, con el ingeniero a la izquierda
  (filete del color de la escudería) y el piloto a la derecha, cada línea con un
  glifo de tres barras que sugiere nivel de audio y el segmento en que se dijo.
- **Rótulo sobre el trazado**: solo para lo que interrumpe, con la forma de la
  banda inferior de la retransmisión. Entra en 200 ms, aguanta 3,5 s y sale en
  250 ms.

El aviso sonoro reutiliza `/audio/sound2.mp3`, el mismo golpe del menú, atenuado
al 35 %. No se añaden ficheros y se respeta el mute y el volumen que el usuario
ya tiene configurados en Ajustes.

La radio sigue al **piloto fijado**; sin fijado, al piloto configurado, que es
del único del que el motor emite telemetría.

## Qué no hace

- No dobla la voz: son textos, no audio generado.
- No decide estrategia. Las paradas y los compuestos los deciden
  `ContextualPitStopPolicy` y `ContextualTireCompoundPolicy` (HU-50 y HU-51); la
  radio se limita a contarlos.
