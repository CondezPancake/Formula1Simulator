# Menú principal e intro

## Qué es

Antes de entrar al shell, la aplicación pasa por dos pantallas nuevas:

1. **Intro** (`IntroController`, Java puro, sin FXML): el logo de F1 aparece
   con fundido y escala sobre un campo de brasas animado. Dura ~5,4 s y se
   puede saltar con clic, `ESC`, `ENTER` o `ESPACIO`.
2. **Menú principal** (`menu.fxml` + `MainMenuController`): hub estilo
   videojuego con fondo de vídeo, cinco tarjetas y salida.

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

## Fondo de vídeo

`simulator/src/main/resources/videos/menu-loop.mp4` (~5 MB, 16 s, 1280×720,
sin audio) se reproduce en bucle y mudo, con comportamiento *cover*: se escala
por el lado que se queda corto y el sobrante se recorta, en vez de dejar bandas
negras.

### El clip original no se versiona

El material de partida (`F1primerclip.mp4`, 410 MB, 15 min) vive en
`media-src/`, que está en `.gitignore`. Estaba en `src/main/resources`, donde
Maven lo copiaba a `target/` en cada compilación y donde habría bloqueado el
push: GitHub rechaza ficheros de más de 100 MB.

Para regenerar el bucle:

```bash
ffmpeg -y -ss 00:02:00 -t 16 -i media-src/F1primerclip.mp4 -an -sn -dn \
  -vf "fps=30,scale=1280:720:flags=lanczos,eq=brightness=-0.08:saturation=0.72:contrast=1.06,format=yuv420p" \
  -c:v libx264 -profile:v high -level 4.0 -pix_fmt yuv420p \
  -crf 27 -preset slow -g 60 -keyint_min 60 -sc_threshold 0 -movflags +faststart \
  simulator/src/main/resources/videos/menu-loop.mp4
```

`-profile:v high -pix_fmt yuv420p` no es opcional: el decodificador de JavaFX
solo acepta H.264 de 8 bits en 4:2:0. El `eq=` acerca el metraje al tono
apagado del mockup, y hacerlo aquí evita pagarlo en cada fotograma en runtime.

### Códec: hace falta `ffmpeg4.4`

`javafx-media` 17.0.10 trae `libavplugin-{54,56,57,58,59}.so`, que abren
`libavcodec.so.{54..59}`. Un Arch al día tiene ffmpeg 9 y **solo**
`libavcodec.so.63`, así que el H.264 no decodifica y salta
`MediaException MEDIA_UNAVAILABLE`. (El MP3 no se ve afectado: lo decodifica
`gstreamer-lite` de forma nativa.)

```bash
sudo pacman -S ffmpeg4.4
pacman -Ql ffmpeg4.4 | grep libavcodec     # confirmar la ruta real
LD_LIBRARY_PATH=/usr/lib/ffmpeg4.4 mvn -f simulator/pom.xml javafx:run
```

`-Djava.library.path` puede no bastar, porque quien hace el `dlopen` es el
propio `.so` y no la JVM; `LD_LIBRARY_PATH` es la vía fiable.

### Respaldo automático

Si el vídeo no está o no decodifica, el menú cae **sin ruido** a
`images/menu-fondo.jpg` con un paneo y zoom lentos (Ken Burns), y sigue siendo
perfectamente usable. Se comprueban todas las vías de fallo —`Media.getError`,
`setOnError`, `setOnHalted`, el estado `HALTED`, el recurso ausente y un
vigilante de 2,5 s que verifica que se llegó a `PLAYING`— porque fallan en
momentos distintos. El `catch` incluye `Error` a propósito: si faltan los `.so`
nativos lo que salta es `UnsatisfiedLinkError`, que no es `RuntimeException`.

Se descartó una secuencia de fotogramas como respaldo: costaba ~84 MB de heap
para algo que, con `ffmpeg4.4` instalado, casi nunca se ve.

`MainMenuController.liberar()` —invocado desde `App` al entrar al shell— suelta
el reproductor y las animaciones; sin eso el vídeo seguiría decodificando
durante toda la sesión.

## Sonido

Solo hay dos efectos, y se usan al elegir opción:

| Fichero | Uso |
|---|---|
| `audio/sound1.mp3` | Confirmar: entrar a una sección principal |
| `audio/sound2.mp3` | Acción secundaria: AJUSTES y SALIR |

**No hay música de fondo**: no se ha entregado ninguna pista. `AudioManager`
sigue ofreciendo `reproducirMusica`/`crossfadeMusica` para cuando se añada.
Toda carga de audio es defensiva: si el fichero falta o no hay códec, se
degrada a silencio y nunca tumba el arranque.

El volumen de música y efectos, y el silencio, se ajustan desde AJUSTES y se
guardan con `java.util.prefs.Preferences`.
