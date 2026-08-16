# feature/data-layer

## Por qué

La capa de persistencia anterior eran 11 archivos de repositorio cuyos métodos **todos lanzaban `UnsupportedOperationException`**: no existía persistencia real. Esta rama la implementa y, de paso, reduce el proyecto a los 5 paquetes objetivo.

## Reconciliar `HashMap` con MongoDB

`f1project.md` pide literalmente «**Map, HashMap:** persistencia temporal de datos», mientras que la decisión del equipo fue mantener MongoDB. Ambas cosas se cumplen sin contradicción:

- **`DataStore` con `ConcurrentHashMap` es la fuente de verdad en tiempo de ejecución** — la «persistencia temporal» de la rúbrica. Todas las lecturas y búsquedas salen de ahí.
- **MongoDB es la persistencia duradera** detrás. Las escrituras son *write-through*: se actualiza el mapa de forma síncrona (la tabla de la interfaz refleja el cambio al instante) y la escritura a Mongo se encola aparte.
- Si Mongo no responde o su base está vacía, se siembra desde `seed.json`. **La aplicación arranca y es plenamente usable sin servidor**, que es lo que la hace demostrable en cualquier máquina.

Se usan colecciones concurrentes porque los hilos de fondo leen mientras el hilo de JavaFX escribe.

### El detalle que hacía falta acertar

El driver de Mongo trae `serverSelectionTimeout` de **30 segundos** y `MongoClients.create()` es perezoso: no falla al construirse, sino en la primera operación. Sin recortarlo, la aplicación se habría quedado bloqueada medio minuto **en cada operación** cuando no hubiera servidor, y el modo memoria no habría servido de nada. `MongoConnection` lo baja a 2 s y expone `isDisponible()`, que hace `ping` y devuelve `false` en vez de lanzar.

## Clases

| Clase | Rol |
|---|---|
| `data/DataStore` | Singleton. Los `Map`, la carga con fallback y las escrituras write-through |
| `data/MongoConnection` | Singleton. Timeout de 2 s + `isDisponible()` |
| `data/CrudRepository<T,ID>` | Contrato del patrón **Repository** |
| `data/MongoRepository<T,ID>` | **Una sola** implementación genérica que sustituye a las 5 parejas interfaz+impl. El mapeo entidad↔`Document` se delega en Jackson, que ya sabe leer el formato de la spec |
| `data/SeedLoader` | Lee `/data/seed.json` |
| `data/DataAccessException` | Renombre de `DatabaseException` |

## Servicios

Cada par `XService` + `XServiceImpl` se colapsa en **una clase concreta**: con una sola implementación, la interfaz solo añadía un archivo y un salto de navegación. La testabilidad se conserva porque el constructor sigue aceptando el `DataStore` inyectado — es justo lo que usan las pruebas vía `DataStore.enMemoria()`.

Se descartó una base genérica `CatalogService<T,ID>`: cada entidad tiene clave, validación y búsqueda propias, así que la base habría añadido indirección sin ahorrar código.

Las 5 excepciones de dominio se reducen a **una** `ValidationException`; el mensaje ya distingue el caso.

Funcionalidad nueva que la rúbrica pedía y no existía:
- **Búsquedas**: circuitos por nombre **o ubicación**, pilotos por nombre/equipo/rol, vehículos por características (texto + velocidad mínima).
- **Asignación de pilotos a vehículos** validando que sean **del mismo equipo**, como exige la HU.
- **Ganadores históricos** resueltos de id a nombre, e **impacto del circuito** sobre consumo y desgaste por modo.

## Datos semilla

`src/main/resources/data/seed.json`: 20 pilotos, **10 equipos**, **10 vehículos** y 7 circuitos. La spec solo define 3 equipos y 2 vehículos, insuficiente para clasificar a los 20 pilotos.

- `RB20` y `W15` reproducen **literalmente** los 18 valores de rendimiento de la especificación (verificado en `SeedLoaderTest`).
- Los otros 8 vehículos se derivan escalando esos valores base entre ×1,02 y ×1,12, con la velocidad del modo agresivo entre 340 y 326 km/h (normal = agresiva − 20, ahorro = agresiva − 40). Da una parrilla con ~5-6 % de dispersión.
- Habilidades y experiencia de los pilotos son **datos de diseño** (la spec no los aporta), en escala 0-100.
- `factor_tecnico` de cada circuito se **calcula** desde su récord real; el resto de factores son datos de diseño.

El archivo se genera con `tools/gen_seed.py`, versionado para que los datos sean reproducibles y auditables en lugar de una tabla escrita a mano.

## Resultado

44 → **37 archivos** `.java`. Paquetes: **`model`, `data`, `service`, `controller`, `util`** — los 5 del objetivo (desaparecen `database/`, `repository/` y `exception/`).

## Verificación

`mvn clean test` → **25 tests, 0 fallos**. Los que más valen:
- `SeedLoaderTest`: se cargan 20/10/10/7; **los 20 pilotos tienen vehículo** (si no, no podrían clasificar); RB20 idéntico a la spec; el `factor_tecnico` guardado coincide con el derivado del récord; y la probabilidad de clima **suma 1 en los 7 circuitos**.
- `CatalogServiceTest`: búsquedas por nombre y ubicación, alta/baja, rechazo de datos inválidos, y que **asignar Hamilton a un Red Bull falla** mientras que asignar Verstappen y Pérez funciona.
