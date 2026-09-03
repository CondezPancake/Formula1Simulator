# feature/ui-shell

## Por qué

La aplicación cargaba `dashboard.fxml` con cinco botones que **no hacían nada**: no existía navegación. Además la carga de datos habría bloqueado el hilo de JavaFX.

## Qué se añadió

- **`shell.fxml`** — marco de la aplicación: navegación lateral, `StackPane` central intercambiable y barra de estado. Es la única escena; las vistas se intercambian dentro.
- **`ShellController`** — lanza la carga inicial en un `Task`, con la navegación deshabilitada y un `ProgressIndicator` visible hasta que termina. En la barra de estado queda el origen de los datos y el recuento de entidades.
- **`Navigator`** — punto de acceso estático para cambiar de vista, más los diálogos de error/aviso/confirmación. Evita ir pasando referencias entre pantallas cuando, por ejemplo, se salta de la lista de circuitos a su detalle.
- **`home.fxml` / `HomeController`** — sustituye a `dashboard.fxml`, con tarjetas de recuento y acceso directo a una nueva clasificación.
- **`App`** — carga el shell y, en `stop()`, cierra el pool de hilos; las conexiones JDBC se cierran por operación.
- **`style.css`** — se amplía con `.nav`, `.card`, `.status-bar`, `.pole-row`, estilos de tabla y de formulario, manteniendo la paleta (#e10600 sobre #15151e). Las vistas no llevan estilos en línea, así que re-skinear al diseño de Figma será cambiar solo este archivo.

## Concurrencia

`DataStore.cargar()` corre en un `Task`; la ventana aparece de inmediato. `setOnSucceeded` puebla la barra de estado y habilita la navegación; `setOnFailed` muestra un diálogo en vez de fallar en silencio. Al ejecutarse dentro de un `Task`, esos callbacks ya están en el hilo de JavaFX y no necesitan `Platform.runLater`.

## Verificación

`mvn javafx:run` arranca sin excepciones y la ventana permanece usable. Medido con una prueba de carga cronometrada en las dos situaciones:

| | Con MySQL | Sin MySQL |
|---|---|---|
| Tiempo de carga | 345 ms | **2,3 s** |
| Origen | MySQL | `seed.json` |
| Datos | 20 pilotos / 10 equipos / 10 vehículos / 7 circuitos | idénticos |

Los 2,3 s del segundo caso son la prueba de que el recorte del `serverSelectionTimeout` funciona: **con el valor por defecto habrían sido 30 s**.

El round-trip relacional conserva las estructuras anidadas: el RB20 recuperado mantiene su consumo en seco (1.9), Mónaco su récord (`1:10.166`), su factor técnico (1.984) y sus 3 ganadores, y los pilotos su rol («Líder») y habilidades.
