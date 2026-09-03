# Rendimiento: carga de imágenes, caché de vistas e hilos

Cambiar de módulo iba lento. El diagnóstico dio tres causas concretas, y las
tres estaban en la presentación: nada que ver con la simulación.

## 1. Las imágenes se abrían a resolución nativa

JavaFX decodifica al tamaño real del fichero salvo que se le pida otro, y
**pedirlo no era lo que hacía el código**:

- La foto más grande del catálogo son 3444×4429, unos **61 MB** ya
  descomprimida, y se abría entera para pintarse en una caja de 210×240.
  `ImageCrop.desdeClasspath` recibía ancho y alto, pero solo los usaba para el
  viewport: al decodificar los tiraba.
- Las banderas son 1000×563 y se abrían así **veinte veces por visita** al
  catálogo, para dibujarse a 34 px.
- El logo de F1 (920×800) se abría dos veces sin compartir nada: una en la
  intro y otra en el menú.

`util/Imagenes` es ahora el punto único de carga. Cachea por **ruta y tamaño**
—la misma foto a 34 y a 300 px son dos mapas de píxeles distintos, y cachear
solo por ruta devolvería el primero que se hubiera pedido— y ofrece una
variante diferida que decodifica fuera del hilo de la interfaz.

> Matiz importante: pedir un tamaño reduce la memoria del resultado, pero
> JavaFX **no submuestrea al decodificar**. El coste de abrir el fichero sigue
> siendo proporcional a su tamaño real; por eso la caché importa tanto como el
> tamaño.

## 2. La caché de vistas estaba invertida respecto al coste

`Navigator` guardaba `simulation` y `gestion`, y recargaba `explorar` desde
FXML en cada visita. Pero `gestion` son cuatro tablas sin imágenes, mientras
que `explorar` es la vista más cara de la aplicación: monta las rejillas con
todas sus tarjetas e imágenes. Ahora `explorar` también se cachea.

## 3. El pool de hilos se quedaba sin sitio

`Async` tenía **dos** hilos. Una sesión de simulación ocupa uno de principio a
fin y su guardado pide otro, así que cualquier otra tarea —cargar datos, abrir
imágenes— esperaba a que terminara la carrera. Ahora son
`max(4, procesadores-1)`, y van numerados: antes los dos compartían nombre y no
se distinguían en un volcado de pila.

## Lo que se dejó fuera a propósito

`SimulationController.mostrarTelemetria` reagrupa toda la telemetría
acumulada **en cada muestra** y reconstruye el selector de vueltas, lo que es
cuadrático sobre la duración de la sesión. Es un cuello de botella real, pero
está en el centro de la simulación y tocarlo arriesga el comportamiento de
telemetría, gráficas y eventos. De ese fichero solo se cambió el decodificado
del mapa del circuito, que es presentación pura.

## Cómo comprobarlo

`ImagenesTest` cubre lo que no se ve a simple vista: que el decodificado
respeta el tamaño pedido, que la caché no confunde dos tamaños del mismo
fichero y que una ruta inexistente devuelve nulo sin reventar. Se verificó en
rojo contra la implementación anterior antes de darlo por bueno.

A mano: entrar y salir de Explorar varias veces debe ser inmediato a partir de
la segunda, y la CPU en reposo no debe quedar elevada.
