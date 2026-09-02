# Circuito dinámico en el Dashboard de Carrera (HU-37)

## Por qué

El Dashboard mostraba **un PNG del trazado**. El propio FXML lo reconocía:

> `<!-- Solo el trazado. Los marcadores se reservan para una HU posterior. -->`

Mientras los veinte pilotos se movían en la torre de tiempos, la pista estaba muerta. En un
simulador que quiere parecerse a un juego de F1, el mapa es justo donde debería verse la carrera.

Ahora el trazado se **dibuja con matemáticas** sobre dos lienzos y los veinte coches lo recorren en
vivo, con el color de su escudería y en el orden exacto de la clasificación.

## El trazado

`model/TrackLayout` es geometría pura, sin una sola importación de JavaFX, para poder comprobarla
sin levantar el toolkit —el mismo criterio que `util/ImageCrop.recorteDe`—.

**Spline Catmull-Rom centrípeta cerrada → Bézier cúbica → reparametrización por longitud de arco.**

- *Catmull-Rom* y no una Bézier suelta porque la curva pasa **por** sus puntos de control: calcar
  un circuito se reduce a colocar puntos sobre él.
- *Centrípeta* (α = ½) y no uniforme: la uniforme forma cúspides y se cruza consigo misma cuando
  los puntos están desigualmente espaciados, que es exactamente lo que pasa en la horquilla de
  Mónaco y en la cuchara de Suzuka.
- *Reparametrización por longitud de arco*: es lo que hace que `puntoEn(t)` avance a velocidad
  constante. Sobre la Bézier cruda el parámetro corre más deprisa en las rectas, así que los
  coches acelerarían al entrar en cada curva.

## De dónde salen los 588 números

No están puestos a mano. `tools/TrazarCircuitos.java` **calca los mapas oficiales** que el proyecto
ya tenía en `/images/circuits/`, y existe por lo mismo que `gen_seed.py`: para que los datos sean
auditables y regenerables. Se ejecuta con `java TrazarCircuitos.java`, sin dependencias.

Máscara de asfalto → cierre morfológico → relleno de huecos → contorno de Moore → desplazamiento
al eje → suavizado cíclico → remuestreo a 84 puntos.

Dos detalles que no son opcionales, y que costaron descubrir:

- **El mapa no dibuja la pista como una banda maciza**, sino como *dos* bandas oscuras con una
  línea clara en medio (8 px + 3 px + 8 px). Sin cerrar ese hueco, cualquier adelgazado devuelve
  las dos orillas en lugar del eje.
- **Los números de curva van en blanco dentro de un disco oscuro**, así que cada badge deja un
  anillo que descarrila el recorrido. De ahí el relleno de huecos pequeños.

Se probó antes a colocar los puntos a ojo: salían trazados que se cruzaban consigo mismos y
ninguno reconocible.

El primer intento usó el **esqueleto** de la banda (adelgazado de Zhang-Suen). Se descartó: el
adelgazado deja bifurcaciones donde las líneas de color cruzan la pista, y el recorrido se atascaba
cubriendo solo la mitad del lazo. El contorno, en cambio, es por construcción una curva cerrada
única y ordenada.

La **meta** y el **sentido de la marcha** los calcula la misma herramienta: la meta es el punto del
lazo más cercano a la bandera a cuadros del mapa, y el sentido sale de comparar el giro del
contorno —Moore siempre sale horario— con el sentido real del circuito. Interlagos y Yas Marina
son los dos que se corren al revés y hay que invertir.

## Dónde va cada coche

Es el punto delicado, porque **el motor no da la posición de nadie en la pista**. Lo único que
varía por piloto a lo largo del tiempo es `ClasificacionEnVivo`: tiempo acumulado, gap y posición.
`segmento/20` es idéntico para los veinte, así que usarlo apila los veinte marcadores en un punto.

`controller/MapaProgreso` lo deriva del **mismo `List<LapResult>` que puebla la torre de tiempos**.
Ese objeto compartido es la garantía de coherencia: el marcador y la fila no son dos cálculos que
puedan divergir, son el mismo dato leído dos veces.

```
base        = segmento / 20
vueltaLider = tiempoLider · 20 / segmento
retrasoReal = gap / vueltaLider                  // el hueco real, a escala
retrasoPiso = SEPARACION_MIN · (posicion − 1)    // 0,011 de vuelta
fraccion    = base − max(retrasoReal, retrasoPiso)
```

**El orden en pista es el de la tabla, y se puede demostrar**: `gap` es no decreciente con la
posición —lo garantiza `ordenarParrilla`— y el piso es estrictamente creciente, luego su máximo lo
es también. Está cubierto por `MapaProgresoTest.elOrdenEnPistaEsElDeLaTorreDeTiempos`, con empates
incluidos, que es donde una fórmula ingenua falla.

El piso existe porque el pelotón real cabe en un 2-3 % de la vuelta y ahí veinte marcadores se
solapan. Solo **añade** separación donde la geometría no la daba; nunca comprime un hueco real, y
por ser función estrictamente creciente de la posición no puede alterar el orden.

Se descartó `tiempoAcumulado / tiempoTotal`: normaliza a cada piloto contra su propio total, así
que los veinte llegan a 1,0 a la vez y el colista se dibuja al lado del poleman.

**Casos borde.** `ordenarParrilla` deja el gap a cero en las vueltas invalidadas, de modo que la
fórmula las apilaría sobre el líder: un coche que ya no gira se **congela** donde estaba, apagado y
sin etiqueta. Y al cortar a mano la sesión el mapa no salta a la meta, porque eso aparentaría que
todos la cruzaron.

## Cómo se mueve con solo 20 fotogramas

`SimulationPacer` coloca el segmento *n* en `duracion·n/20`: medio segundo con la duración por
defecto, tres minutos con la máxima. Repintar solo en cada aviso daría un movimiento a saltos.

El estado se publica como `record` inmutable y un `AnimationTimer` interpola con `smoothstep`.

- Al publicar se parte de **donde están los coches ahora**, no del objetivo anterior. Con eso, un
  segmento que llega tarde o repetido no da tirón ni rebote.
- La duración del tramo se acota a `[80 ms, 1200 ms]`. El tope importa: un *lerp* de 180 segundos
  sería indistinguible de una interfaz congelada, y encima quemaría un pulso cada 16 ms para no
  mover nada.
- La interpolación **toma el arco corto** (`delta -= floor(delta + 0,5)`). Sin eso, un coche que
  cruza la meta entre dos segmentos (0,98 → 0,02) barrería el trazado entero hacia atrás.

## El render

Dos lienzos apilados: abajo la pista, que se dibuja **una vez** por circuito y tamaño; arriba solo
los veinte marcadores. Repintar la pista sesenta veces por segundo sería gastar el presupuesto en
algo que no cambia.

Tres detalles que no se pueden copiar del precedente de `IntroController`:

- **`setManaged(false)` en los dos lienzos.** Un `Canvas` devuelve su propio tamaño como tamaño
  preferido y un `StackPane` se dimensiona a partir de sus hijos: dejarlo gestionado y a la vez
  atado al padre realimenta el bucle y el panel crece sin parar. `IntroController` se libra porque
  a su `StackPane` lo dimensiona la ventana.
- **La capa de coches se limpia con `clearRect`**, no velando con negro translúcido como hace la
  intro: este lienzo es transparente y el velo apagaría la pista de debajo.
- **La tipografía del lienzo es Titillium Web.** `JetBrains Mono`, que usa el CSS, no está
  empaquetada: solo se cargan las cuatro Titillium en `App`.

El orden de dibujo reproduce el de los mapas oficiales: borde blanco por debajo → cinta de asfalto
→ tinte de sector al 30 % → meta a cuadros rotando el `GraphicsContext`. Los cortes de sector son
los que usa el motor (`TrackSector.desdeSegmento`), no unos inventados.

## La fuga que había que evitar

`Navigator` guarda referencias duras a la vista de simulación y a su controlador para toda la vida
de la aplicación, y no hay ningún `dispose()`. Un `AnimationTimer` arrancado aquí no se destruiría
nunca al salir de Carrera.

Se ata al `sceneProperty()` del lienzo —el precedente está en `IntroController`—, que se pone a
nulo en cuanto `Navigator` desengancha el nodo, más una bandera que solo permite animar mientras
hay sesión viva. Un mapa terminado cuesta cero CPU.

## El Dashboard

La tarjeta del **compañero de equipo se movió a la pestaña de telemetría** para hacer sitio a los
cuatro paneles de sesión (vuelta, clima, leyenda y mejor vuelta). Es la que debía ceder: sus
lecturas no vienen del motor, sino de las del piloto seleccionado multiplicadas por un factor y una
oscilación senoidal. Con veinte coches reales y una torre de tiempos real en pantalla, un panel
derivado es lo primero que sobra.

La torre y el mapa quedan enlazados en los dos sentidos: pasar por encima de una fila resalta su
marcador, pasar por encima de un marcador resalta su fila, y pulsarlo abre la ficha del piloto.

`.race-map-canvas` sube de 260 a 320 px de alto mínimo: a 260, con los coches repartidos, los
marcadores quedaban a menos de su propio diámetro unos de otros.

## Lo que no se hizo, y por qué

**No se marca la curva del incidente.** `EventOccurrence.sector` se sortea al azar entre los tres
sectores y no guarda ninguna relación con la geometría, así que señalar una curva concreta sería
inventar una correspondencia que el motor no tiene, y contradiría a la pestaña de eventos.

**Suzuka no cruza por encima de sí mismo.** El paso elevado del ocho se calca como un lazo simple:
extraerlo exigiría resolver la ambigüedad del cruce en la imagen.

## Verificación

`./mvnw -f simulator/pom.xml test` → **150 pruebas, 0 fallos**.

- `TrackLayoutTest` (9), sin entorno gráfico: cierre del lazo sin costura, avances iguales del
  parámetro recorriendo distancias iguales, la curva pasando por sus puntos de control, el encaje
  respetando el lienzo y la proporción, y los puntos duplicados sin romper la fórmula centrípeta.
- `TrackLayoutsTest` (4): los siete circuitos del catálogo tienen trazado —si alguien renombra uno
  en `gen_seed.py`, el mapa se quedaría en blanco sin avisar y esta prueba lo convierte en un fallo
  ruidoso—, todos son apaisados, y los cortes de sector se derivan de `TrackSector` en vez de
  fijar 0,35 y 0,70 a mano.
- `MapaProgresoTest` (9): la invariante del orden, el líder en cabeza, la parrilla empatada sin
  solapes, el hueco grande a escala real, el congelado de la vuelta invalidada y del piloto fuera,
  y el arco corto al cruzar la meta.
- `ViewsLoadTest` sigue cargando `simulation.fxml` de verdad, que es lo que caza un `fx:id` mal
  puesto.

## Nota de entorno

Maven no estaba instalado. Se añadió el **wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`, Maven
3.9.9): ahora basta un JDK 17 para clonar y ejecutar. `run.sh` lo usa y deduce `JAVA_HOME` del
`java` del PATH si no está definido.
