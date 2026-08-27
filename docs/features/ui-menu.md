# Menú principal e intro

## Qué es

Antes de entrar al shell, la aplicación pasa por dos pantallas nuevas:

1. **Intro** (`IntroController`, Java puro, sin FXML): el logo de F1 aparece
   con fundido y escala sobre un campo de brasas animado. Dura ~5,4 s y se
   puede saltar con clic, `ESC`, `ENTER` o `ESPACIO`.
2. **Menú principal** (`menu.fxml` + `MainMenuController`): hub estilo
   videojuego con fondo de fotos en slideshow, cinco tarjetas y salida.

`App` encadena intro → menú → shell sobre una única `Scene`, cruzando con
fundidos de 400 ms. Mantener un solo `Stage`/`Scene` evita el parpadeo y el
salto de tamaño que provocaría recrear la ventana estando maximizada.

## Geometría: por qué no hay `HBox`

El diseño de referencia es `docs/assets/menu_mockup.png` (1672×941). Las cinco
tarjetas **no** forman una rejilla: tienen anchos distintos (las tres oscuras
se estrechan de izquierda a derecha), alturas distintas y bordes superiores
distintos. Ningún contenedor estándar reproduce eso, así que van en un `Pane`
con los hijos sin gestionar (`setManaged(false)`) y recolocados con
`resizeRelocate` a partir de fracciones del lienzo.

> No se usa `layoutXProperty().bind(...)`: `Pane.layoutChildren()` llama a
> `relocate()` sobre los hijos gestionados, lo que escribiría sobre una
> propiedad enlazada y lanzaría una excepción.

El **chaflán** (corte a 45° de 20 px en la esquina superior derecha) es un
`Polygon` puesto como `clip`, con los puntos recalculados en cada cambio de
tamaño para que el corte mida siempre lo mismo. Como el `clip` se aplica
*después* del `effect`, el resplandor rojo de la tarjeta principal va en un
`StackPane` envoltura y no en el `Button` recortado; si se pusieran juntos, el
recorte se comería el resplandor. Por lo mismo, el contorno gris de AJUSTES es
un `Polygon` hermano y no un borde CSS.

## Tipografía

JavaFX 17 **no tiene `-fx-letter-spacing`** (solo `-fx-line-spacing`), así que
el tracking de `TEMPORADA 2025`, la línea de contadores y `SALIR` se compone
carácter a carácter en un `HBox` de nodos `Text`.

Los cuerpos de letra no están en el CSS: los calcula el controlador desde la
altura de la escena, porque el mockup escala la tipografía con la ventana.

El mockup usa una condensada que Titillium Web no es. Reproducir su métrica
exacta exigiría comprimir al ~54 % y deformaría la letra, así que se reproduce
el efecto —título grande, de sangrado a sangrado— con una compresión acotada
(`Scale` con pivote en el borde izquierdo) y, si aun al máximo no cabe, se baja
el cuerpo lo justo. Sin `setMinWidth(USE_PREF_SIZE)` el `Label` se recortaría
con puntos suspensivos: la `Scale` comprime lo que se ve, pero no reduce los
límites de layout.

Los iconos son `SVGPath` escritos a mano en caja `0 0 24 24`, escalados con un
`Scale` enlazado a la altura de la escena. Viven en el FXML para que
`ViewsLoadTest` valide que las rutas parsean.

## Fondo: slideshow de fotos

El fondo del menú fue en su día un vídeo en bucle; se retiró porque cargaba la
CPU de forma notoria. Hoy `MainMenuController.montarFondoSlideshow()` cicla
entre hasta tres fotos (`images/menu-fondo/fondo-01/02/03.jpg`), cada una con
comportamiento *cover* (se escala por el lado que se queda corto y el
sobrante se recorta, igual que hacía el vídeo).

Ritmo, centralizado en `util/Animaciones`:

- Cada foto se mantiene `FONDO_HOLD` (3 s).
- Al cambiar, la siguiente foto entra encima con opacidad 0 y funde a 1 en
  `FONDO_CROSSFADE` (1000 ms) —el mismo patrón aditivo que usa `App.cruzar()`—
  y solo entonces se retira la anterior del `StackPane`.
- Cada foto tiene su propio *Ken Burns*: un `Timeline` de una sola pasada
  (sin `autoReverse` ni ciclo infinito, a diferencia del respaldo estático que
  tenía el vídeo) que escala de 1.00 a 1.03 durante exactamente el tiempo que
  la foto está visible, para que el zoom se note pero termine justo cuando
  sale de escena.

Si falta algún fichero se salta sin más; si faltan los tres, `capaFondo` se
queda con un `Region` vacío y el scrim ya deja el fondo en negro —la misma
degradación silenciosa que tenía el respaldo del vídeo.

`MainMenuController.liberar()` —invocado desde `App` al salir del menú— para
el `Timeline` del ciclo y el Ken Burns en curso; sin eso seguirían corriendo
de fondo durante toda la sesión.

## Interacción del menú

**Hover**: la tarjeta bajo el cursor escala a 1.03 (`Animaciones.HOVER` =
180 ms, `EASE_OUT`) y las otras cuatro bajan a opacidad 0.72, para dar foco a
la activa. El aumento de brillo y el acento rojo más visible los cubre el CSS
`:hover` que ya existía para cada variante de tarjeta; la roja además gana un
glow más intenso (`.menu-tile-wrap-primary:hover`).

Todo eso lo decide **un solo método**, `resaltar(activa)`, que recalcula el
estado de las cinco tarjetas a partir de cuál está bajo el cursor —con
`activa == null` cuando no hay ninguna—. Repartirlo entre un manejador de
entrada y otro de salida daba dos problemas: al pasar de una tarjeta a la
vecina, las tres ajenas recibían a la vez un destino de opacidad por cada
evento y se peleaban; y si la salida no llegaba nunca (un modal que se abre
encima, el cursor que reaparece en otro punto) la tarjeta activa se quedaba
atenuada. `MenuHoverTest` cubre el segundo caso, que es el que falla de forma
determinista. Cada tarjeta reutiliza una única transición de opacidad y otra
de escala, que se detienen antes de redirigirse.

**Elegir una sección** (CLASIFICACIÓN/GESTIÓN/EXPLORAR/HISTORIAL): la tarjeta
pulsa (escala a 1.06, `Animaciones.PULSO_TILE` = 110 ms) y `App` entra al
shell con `cruzarSeccion()`, que **desliza sin fundir**: la pantalla entrante
es opaca —se lo da la regla `.root`— y pasa por encima de la saliente, que se
retira ya tapada. Antes esto cruzaba opacidades y además apagaba el menú a
0.25/0.35: las dos pantallas quedaban medio transparentes a la vez y el fondo
casi negro de la raíz asomaba entre ambas, que es exactamente el bajón a
negro que se quería evitar. La confirmación se lee igual de bien solo con la
escala. Pulso + slide (`Animaciones.TRANSICION_SECCION`, 380 ms) ≈ 490 ms.
AJUSTES recibe solo el pulso: abre un diálogo modal sobre el propio menú, no
navega a ninguna parte.

**Volver al menú**: el "F1" de la cabecera del shell (antes un `Label`, ahora
un `Button`, ver `ShellController.onVolverAlMenu`) llama a
`App.mostrarMenu(raiz, true)`, que reutiliza `cruzarSeccion()` en sentido
inverso. El menú se recarga desde FXML igual que la primera vez, así que el
slideshow **arranca de nuevo desde la primera foto** en vez de recordar en
qué imagen se quedó —cachear ese estado exigiría un cambio de arquitectura
(igual al de `Navigator` con sesión/gestión) desproporcionado para una
diferencia cosmética.

## Entrada de pantallas dentro del shell

`Navigator.mostrarConEntrada()` envuelve los cuatro sitios donde antes se
hacía `contenedor.getChildren().setAll(...)` a secas: la vista nueva entra con
opacidad 0→1 y `translateY` 15px→0 en `Animaciones.ENTRADA_PANTALLA` (360 ms,
`EASE_OUT`). Se aplica igual a una vista recién cargada por FXML que a una
restaurada de caché (sesión de simulación, gestión): en ambos casos el
usuario "llega" a la pantalla. No hay *stagger* interno (título primero,
contenido después): tocar cada vista individualmente para eso no compensaba
frente a una entrada uniforme del contenedor.

## Sonido

| Fichero | Uso |
|---|---|
| `audio/sound1.mp3` | Confirmar: entrar a una sección principal del menú |
| `audio/sound2.mp3` | Acción secundaria: AJUSTES y SALIR, y hover de tarjeta al 35 % |
| `audio/sound-intro.mp3` | Stinger corto al arrancar la intro |
| `audio/intro-f1.mp3` | Tema de fondo de la intro, sin loop (dura lo que dura la intro) |

Los dos de la intro se lanzan juntos al construir `IntroController`, y se
cortan (`AudioManager.detenerMusica()`) en el mismo punto de salida que ya
cubre tanto el fin natural como el salto (`terminarUnaVez`, protegido por un
`AtomicBoolean`).

`AudioManager` **cachea cada efecto**: construir un `AudioClip` decodifica el
mp3 en el hilo llamante, que es el de FX, así que hacerlo en cada clic y en
cada hover metía un tirón justo cuando arrancaba la animación. El hover suena
además atenuado (35 %) y con una espera mínima de 350 ms entre disparos, para
que acompañe sin tapar el sonido de confirmación al elegir sección.

**No hay música de fondo del menú/shell**: no se ha entregado ninguna pista.
`AudioManager` sigue ofreciendo `reproducirMusica`/`crossfadeMusica` para
cuando se añada. Toda carga de audio es defensiva: si el fichero falta o no
hay códec, se degrada a silencio y nunca tumba el arranque.

El volumen de música y efectos, y el silencio, se ajustan desde AJUSTES y se
guardan con `java.util.prefs.Preferences`.
