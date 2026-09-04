# Explorar: Pilotos y Teams

## Qué es

`explorar.fxml` es un `TabPane` con dos pestañas: **PILOTOS** y **TEAMS**.
Antes había además GARAJE y CIRCUITOS; se retiraron junto con sus vistas,
sus controladores y `VehicleGallery`, que se quedaban sin punto de entrada.
Los listados de vehículos y circuitos de **GESTIÓN** son otros y siguen ahí.

## Tarjetas de escudería

`ExploreTeamsController` reproduce el reparto de formula1.com/en/teams: fondo
en el color del equipo, banda diagonal de acento, logo y nombre arriba, el
monoplaza cruzando la parte baja y la pareja de pilotos a la derecha.

Es el mismo patrón que la rejilla de pilotos —tarjeta de tamaño fijo con un
lienzo interior recortado y el halo del hover en el nodo de fuera, para que el
recorte no se coma el resplandor— y se apoya en lo que ya existía:

| Necesidad | De dónde sale |
|---|---|
| Equipos, búsqueda y sus dos pilotos | `TeamService.listar/buscar/pilotosDe` |
| Color vivo y color accesible | `TeamColors.hex/accesible` |
| Logo | `F1Assets.logo(nombre)` |
| Render del piloto | `F1Assets.render(codigo)` |
| Monoplaza | `VehicleService.delEquipo(nombre)` |

`F1Assets.logo` existía pero no lo llamaba nadie; esta pantalla es su primer
consumidor.

## Ficha de equipo

`team-detail` se abre con `ExploreTeamsController.abrirFicha(nombre)`, que
navega con `Navigator.irConRetorno` y llama a `mostrar(nombre)`. Trae el logo
en grande, el coche con sus datos reales —punta, 0-100, motor— y los dos
pilotos, cada uno enlazando con la ficha de piloto ya existente.

`Navigator.volver()` restaura el nodo exacto del catálogo con su pestaña
seleccionada, así que no hace falta nada más para regresar.

> `ShellController.sincronizarVista` lleva una lista blanca que decide qué
> botón de la barra queda resaltado. `explore-teams` y `team-detail` tienen que
> estar en ella o EXPLORAR pierde el resaltado al abrir una ficha.

## Los logos

Los de `/images/teams/*.png` son los oficiales, en 512×512 con transparencia.
Los que había antes eran de 96×96 y ~1 KB, y en una tarjeta se veían borrosos.

`tools/descargar_logos.sh` los vuelve a bajar. Dos detalles que no son
opcionales:

- Se piden en **PNG** (`f_png` en la URL): el sitio sirve WebP por defecto y
  **JavaFX no lo decodifica**.
- Cada logo se baja a un temporal que solo sustituye al bueno si la descarga
  cuela, para no dejar un fichero a medias. Sin red, la app conserva los que ya
  tiene.

El mapa de nombres traduce el seed al sitio de F1. Dos no coinciden porque esas
escuderías cambiaron de nombre: **Alfa Romeo → `audi`** y
**AlphaTauri → `racingbulls`**.

## Por qué los coches y los pilotos son locales

El sitio oficial ya va por la parrilla de **2026** y nuestro `seed.json` es la
de **2024**. Bajar coches y caras de allí contradiría nuestros propios datos:
saldría un coche de 2026 para el modelo `RB20`, y Pérez aparecería en Cadillac
cuando el seed lo tiene en Red Bull. Los assets locales sí corresponden, así
que solo los logos —que son de equipo y aguantan el cambio de año— vienen de
fuera.
