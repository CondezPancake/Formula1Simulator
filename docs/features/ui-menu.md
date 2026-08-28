# Menú principal e intro

## Qué es

Antes de entrar al shell, la aplicación pasa por dos pantallas:

1. **Intro** (`IntroController`, Java puro, sin FXML): el logo de F1 aparece
   con fundido y escala sobre un campo de brasas animado. Dura ~6 s y se
   puede saltar con clic, `ESC`, `ENTER` o `ESPACIO`.
2. **Menú principal** (`menu.fxml` + `MainMenuController`): reproducción del
   menú del videojuego **F1 23**.

`App` encadena intro → menú → shell sobre una única `Scene`.

## Sin transiciones

Los cambios de pantalla son **relevos secos** (`App.mostrar()`, un
`getChildren().setAll(...)`), y dentro del shell también
(`Navigator.mostrarVista()`). No hay fundidos ni deslizamientos en ningún
punto: se retiraron a propósito. La intro conserva su animación interna,
que es su razón de ser, y como termina desvaneciéndose sola antes de invocar
su callback, el corte hacia el menú no llega a verse.

La única animación que queda en toda la aplicación es la de la intro.

## El menú

Referencia: `docs/assets/f1-23-menu-referencia.jpg`.

`GridPane` con dos columnas de `percentWidth` 47/53, la proporción de la
captura. Es una rejilla de verdad, a diferencia del menú anterior, que
posicionaba tarjetas por fracciones del lienzo porque no formaban rejilla.

**Columna izquierda** — logo + `TEMPORADA 2025`, la lista de cinco opciones,
la descripción de la resaltada y, abajo, `SALIR` con la barra de pistas de
teclado.

Cada fila se compone de dos piezas con una responsabilidad distinta:

- la **fila** (`HBox`) ocupa todo el ancho y es la zona sensible al ratón, para
  que el cursor la coja entera y no solo encima de la palabra;
- el **marco** (`StackPane` interior) ciñe al texto y es quien recibe la clase
  `menu-opcion-activa`. En la referencia el recuadro termina justo después de
  la palabra, no en el borde de la columna.

A la derecha del marco van tres **galones** (`SVGPath`) de opacidad
decreciente, visibles solo en la fila activa: es el rasgo que más identifica
al menú de F1 23.

**Columna derecha** — el arte de la opción activa, a sangre, con la tira de
contadores arriba a la derecha y el título de la sección abajo. Cada opción
lleva su imagen (`images/menu-<opcion>.{png,jpg}`) y el panel la cambia al
instante al moverse por la lista: sigue sin haber animación.

Para que no parezca una lámina pegada, la imagen se encaja recortando —con el
recorte sesgado hacia arriba, porque `menu-explorar.png` es vertical—, un
`PerspectiveTransform` recoge las dos esquinas del lado izquierdo para que el
plano se lea inclinado hacia la mitad negra, y un velo funde ese borde con el
panel y oscurece la parte baja bajo el título.

> Dos trampas que costaron una pasada. El margen estaba en el panel, así que
> arte y velo se quedaban dentro del hueco y aparecía un marco del fondo
> alrededor: ahora va en el contenido. Y el arte no aparecía hasta mover el
> ratón, porque se pintaba desde el escalado —que se dispara con el tamaño de
> la raíz— y el `GridPane` reparte el ancho a sus columnas en una pasada
> posterior, cuando el panel derecho todavía mide cero; se escucha el tamaño
> del propio panel.

> **El orden de las capas de `-fx-background-color` importa y va al revés que
> en CSS de web**: en JavaFX el primer fondo de la lista se pinta *abajo* y los
> siguientes encima. Con la base opaca al final tapaba las franjas y el
> resplandor, y el panel se veía plano.

Los contadores (`21 PILOTOS · 11 EQUIPOS · …`) salen de `DataStore` y se
pintan **una sola vez**. Si la carga aún no ha terminado quedan guiones: la
intro dura ~6 s y la carga corre en paralelo desde `App.start()`, así que en
la práctica siempre está lista, y montar un temporizador para un dato
decorativo contradiría el «menú estático».

## Interacción

Ratón y teclado, como un menú de consola: el cursor solo **mueve** el
resaltado y el clic **confirma**; `↑`/`↓` (o `W`/`S`) recorren la lista con
envolvente, `ENTER`/`ESPACIO` confirman y `ESC` va a SALIR.

> El teclado se engancha con un **filtro en la escena**, no con un manejador en
> la raíz. Un manejador en la raíz solo dispara si el foco está justamente
> ahí, y basta que lo tenga el botón SALIR para que las flechas dejen de
> responder. Como la aplicación reutiliza una única `Scene` para todas las
> pantallas, el filtro **se retira** cuando el menú la abandona; si no,
> seguiría interceptando las flechas dentro del shell.

Las cinco opciones y su destino, en el orden en que aparecen:

| Opción | Destino |
|---|---|
| CLASIFICACIÓN | `ShellController.irACarrera()` → vista `simulation` |
| GESTIÓN DE EQUIPOS | `ShellController.irAGestion()` → vista `gestion` |
| EXPLORAR | `ShellController.irAExplorar()` → vista `explorar` |
| HISTORIAL | `ShellController.irAHistorial()` → `config-historial`, pestaña Historial |
| AJUSTES | `AjustesDialog.mostrar()` (modal; no navega) |

El destino no se ejecuta directamente: se pasa a `App`, que se lo entrega a
`ShellController.arrancar(destino)` para que el shell abra ya en la sección
elegida. Ese rodeo existe porque, si los datos aún no han terminado de
cargarse, el shell remataba volviendo a Carrera y se llevaba por delante la
elección del usuario.

`MenuNavegacionTest` cubre el orden de las opciones, la envolvente del teclado
y que el panel derecho siga a la opción activa; se comprobó que falla contra
implementaciones defectuosas antes de darlo por bueno.

## Tipografía

Los cuerpos de letra no están en el CSS: los calcula el controlador desde la
altura de la escena, porque el menú escala con la ventana.

> Hay un tope de cordura (`medidaValida`, 8000 px). En algunos compositores
> (visto en Hyprland/Wayland) `Stage.setMaximized` hace que, durante un único
> pulso, la ventana informe un alto disparatado —miles de millones de px—
> antes de que llegue el real. Sin el tope ese valor se propagaba a los
> cuerpos de letra y reventaba el cálculo interno de ajuste de texto de
> JavaFX de forma permanente: la pantalla se quedaba en blanco y no se
> recuperaba sola.

El tracking de `TEMPORADA 2025` se compone carácter a carácter en un `HBox` de
nodos `Text`: JavaFX 17 no tiene `-fx-letter-spacing`, solo `-fx-line-spacing`.

## Sonido

| Fichero | Uso |
|---|---|
| `audio/sound1.mp3` | Confirmar una opción |
| `audio/sound2.mp3` | Recorrer la lista (al 35 %) y SALIR |
| `audio/intro-sound.mp3` | Sonido de apertura de la intro |

El de la intro va por `reproducirMusica` y no por `reproducirSfx`: un
`AudioClip` no se puede parar a media reproducción, así que al saltar la intro
el sonido seguiría oyéndose ya dentro del menú. Se corta en `terminarUnaVez`.

> La intro estuvo muda mucho tiempo apuntando a `sound-intro.mp3` e
> `intro-f1.mp3`, que nunca se empaquetaron. Como la carga de audio degrada a
> silencio a propósito, una ruta mal escrita no da ningún error: simplemente no
> se oye. `RecursosAudioTest` comprueba que las rutas existen.

Toda carga de audio es defensiva —si el
fichero falta o no hay códec, se degrada a silencio y nunca tumba el arranque—
y `AudioManager` cachea cada efecto, porque construir un `AudioClip`
decodifica el mp3 en el hilo de FX y hacerlo en cada pulsación metía un tirón.

El volumen y el silencio se ajustan desde AJUSTES y se guardan con
`java.util.prefs.Preferences`.
