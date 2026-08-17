<p align="center">
  <img src="docs/assets/logo.svg" width="640" alt="Formula1Simulator logo">
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-e10600?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="JavaFX" src="https://img.shields.io/badge/JavaFX-17.0.10-e10600?style=flat-square&logo=java&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-build-e10600?style=flat-square&logo=apachemaven&logoColor=white">
  <img alt="MongoDB" src="https://img.shields.io/badge/MongoDB-5.1.1-e10600?style=flat-square&logo=mongodb&logoColor=white">
  <img alt="Tests" src="https://img.shields.io/badge/tests-42%20passing-2ea043?style=flat-square">
</p>

# Formula1Simulator

Aplicación de escritorio en **Java 17 + JavaFX** para simular una sesión de **clasificación de Fórmula 1**: gestión de pilotos, equipos, vehículos y circuitos, configuración del monoplaza, y una sesión en la que los 20 pilotos marcan tiempo según su habilidad, su coche, el circuito, el clima y los ajustes elegidos.

La especificación que sigue el proyecto es [`f1project.md`](f1project.md).

---

## Tabla de contenidos

- [Estado](#estado)
- [Funcionalidades](#funcionalidades)
- [Cómo funciona la simulación](#cómo-funciona-la-simulación)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Datos](#datos)
- [Flujo de trabajo Git](#flujo-de-trabajo-git)
- [Historial de cambios](#historial-de-cambios)
- [Pendiente](#pendiente)

---

## Estado

**Funcional de punta a punta.** Las 22 historias de usuario de `f1project.md` están implementadas: CRUD con búsqueda de las cuatro entidades, configuración del vehículo, simulación de clasificación con hilos y almacenamiento de resultados y configuraciones.

La aplicación **arranca y es plenamente usable con o sin MongoDB**: si no hay servidor, se siembra desde un JSON incluido y todo funciona en memoria.

| | |
|---|---|
| Archivos `.java` | 46 |
| Paquetes | 5 |
| Patrones de diseño | 2 (Repository, Singleton) |
| Tests | 42, todos en verde |

## Funcionalidades

**Gestión de datos** — Alta, consulta, edición y baja de pilotos, equipos, vehículos y circuitos, con búsqueda en vivo: pilotos por nombre/equipo/rol, circuitos **por nombre o ubicación**, vehículos **por características** (texto y velocidad mínima). Asignación de pilotos a un vehículo restringida a los de su misma escudería.

**Análisis** — Comparación de dos o más vehículos en una tabla transpuesta con gráfico de barras. Ficha de circuito con récord de vuelta, ganadores históricos, clima promedio e impacto de la pista sobre consumo y desgaste.

**Simulación** — Clima aleatorio según la distribución del circuito, cálculo del tiempo de los 20 pilotos, parrilla ordenada con la pole destacada y diferencias respecto a ella. La vuelta del piloto seleccionado muestra en vivo velocidad, consumo acumulado y desgaste. Todo corre en segundo plano sin congelar la ventana.

**Historial** — Sesiones guardadas con su parrilla, comparación de tiempos de pole entre sesiones del mismo circuito, y configuraciones previas reutilizables.

## Cómo funciona la simulación

```
t_base   = 3600 · longitud_km / velocidad_promedio(modo)
t_vuelta = t_base · factorTecnico · f_clima · f_aero · f_presion
                  · f_combustible · f_piloto · (1 ± 0,5 %)
```

El **factor técnico** de cada circuito no es un número inventado: se deriva de su récord de vuelta real comparándolo con el tiempo a velocidad de referencia, lo que sitúa a Mónaco en 1,98 y a Monza en 1,32. Cada ajuste tiene contrapartida —más carga aerodinámica mejora el tiempo pero dispara el consumo; menos presión mejora el agarre pero desgasta más— para que configurar importe.

Una parrilla típica en Monza:

```
 1. Max Verstappen    Red Bull Racing   RB20    1:16.453       —
 2. Sergio Pérez      Red Bull Racing   RB20    1:16.576  +0.124
 3. Carlos Sainz      Ferrari           SF-24   1:16.711  +0.259
 …
20. Logan Sargeant    Williams          FW46    1:20.859  +4.407
```

## Tecnologías

**Java 17** · **JavaFX 17.0.10** (controls + fxml) · **MongoDB** (driver síncrono 5.1.1) · **Jackson** 2.17.2 · **Maven** · **JUnit 5**

## Arquitectura

Cinco paquetes y **dos patrones de diseño**, los mínimos que aportan valor real:

| Patrón | Dónde | Por qué |
|---|---|---|
| **Repository** | `data.CrudRepository` + `data.MongoRepository<T,ID>` | Aísla MongoDB del resto y permite que la aplicación funcione sin él |
| **Singleton** | `data.MongoConnection`, `data.DataStore` | Un solo cliente de Mongo (es caro y thread-safe por diseño) y un único almacén compartido |

### Datos: `HashMap` sobre MongoDB

`f1project.md` pide «Map, HashMap: persistencia temporal de datos». Ambas cosas conviven sin contradicción:

- **`DataStore` con `ConcurrentHashMap` es la fuente de verdad en ejecución.** Todas las lecturas y búsquedas salen de ahí.
- **MongoDB es la persistencia duradera.** Las escrituras actualizan el mapa de forma síncrona (la tabla responde al instante) y se propagan a Mongo aparte.
- Si Mongo no responde o su base está vacía, se siembra desde `seed.json`.

`MongoConnection` recorta el `serverSelectionTimeout` del driver de 30 s a 2 s y expone `isDisponible()`. Sin ese ajuste la aplicación se bloquearía medio minuto en cada operación cuando no hubiera servidor.

### Concurrencia

Un único pool de dos hilos demonio (`util.Async`). La carga inicial y la simulación corren en `javafx.concurrent.Task`, con la interfaz **enlazada** a sus propiedades de progreso y mensaje. Ningún acceso a datos ni cálculo ocurre en el hilo de JavaFX.

## Estructura del proyecto

```text
Formula1Simulator/
├── f1project.md                 # Especificación del proyecto
├── docs/
│   ├── features/                # Un .md por rama: qué se hizo y por qué
│   └── legacy/                  # Especificación anterior, ya no vigente
├── tools/gen_seed.py            # Genera seed.json de forma reproducible
└── simulator/
    ├── pom.xml
    └── src/
        ├── main/java/com/formula1/
        │   ├── App.java, Main.java
        │   ├── model/      # entidades + enums con sus factores
        │   ├── data/       # DataStore, MongoConnection, repositorios, seed
        │   ├── service/    # CRUD, búsquedas, motor de clasificación
        │   ├── controller/ # controladores JavaFX, navegación y formularios
        │   └── util/       # formato, validación, aleatoriedad, hilos
        ├── main/resources/
        │   ├── views/      # 10 vistas FXML
        │   ├── css/style.css
        │   └── data/seed.json
        └── test/java/      # 40 tests
```

## Instalación y ejecución

```bash
cd simulator

mvn compile      # compilar
mvn javafx:run   # ejecutar
mvn test         # pruebas
```

MongoDB es **opcional**. Por defecto se conecta a `mongodb://localhost:27017`; se puede cambiar con las variables de entorno `MONGO_URI` y `MONGO_DATABASE`. Sin servidor, la aplicación arranca igual en modo memoria y lo indica en la barra de estado.

## Datos

`seed.json` contiene 20 pilotos, 10 equipos, 10 vehículos y 7 circuitos. La especificación define literalmente los 20 pilotos y 7 circuitos, pero solo 3 equipos y 2 vehículos —insuficiente para que los 20 pilotos clasifiquen—, así que se completó la parrilla:

- **RB20 y W15 reproducen exactamente** los 18 valores de rendimiento de la especificación (verificado en `SeedLoaderTest`).
- Los otros 8 vehículos se derivan escalando esos valores base, con velocidades entre 340 y 326 km/h en modo agresivo.
- Habilidades y experiencia de los pilotos, y los factores de clima/consumo/desgaste de los circuitos, son **datos de diseño**: la especificación no los aporta.

El archivo se genera con `tools/gen_seed.py` para que los datos sean reproducibles y auditables.

## Flujo de trabajo Git

`develop` es la rama de integración. Cada pieza se implementa en su rama `feature/*` o `refactor/*`, con commits en **Conventional Commits + gitmoji**, un `docs/features/<nombre>.md` explicando el porqué, y merge a `develop` con `--no-ff`.

## Historial de cambios

| Commit | Rama | Qué aportó |
|---|---|---|
| `feat: 🎉 Structure project_initializate` | — | Inicialización del repositorio |
| `feat: 📦 Configurar dependencias y plugins de Maven` | `feature/pom-setup` | JavaFX, MongoDB, Jackson, JUnit |
| `feat: 🧱 Crear modelo de dominio` | `feature/model` | Primeras entidades |
| `feat: 🔧 Crear excepciones y utilidades` | `feature/exceptions-util` | `util/` y excepciones |
| `feat: 🗃️ Crear conexión Mongo y repositorios` | `feature/database-repository` | Esqueleto de persistencia |
| `feat: 🛰️ Crear adapter de OpenF1` | `feature/api-openf1` | *(retirado después)* |
| `feat: ⚙️ Crear capa de servicios` | `feature/service` | Esqueleto de servicios |
| `feat: 🏁 Crear motor de simulación y facade` | `feature/simulation-engine` | Esqueleto del motor |
| `feat: 🎮 Crear controllers de JavaFX` | `feature/controllers` | Controladores iniciales |
| `feat: 🎨 Crear bootstrap de JavaFX` | `feature/javafx-bootstrap` | Arranque y vistas placeholder |
| `docs: 📝 Crear README` | `feature/readme` | Documentación inicial |
| **`refactor: 🔥 Recortar el alcance a f1project.md`** | `refactor/scope-cleanup` | Elimina OpenF1, Q1/Q2/Q3 y telemetría: 64 → 44 archivos, 4 → 2 patrones |
| **`feat: 🧱 Rediseñar el modelo`** | `feature/model-redesign` | Encaje 1:1 con el JSON de la spec |
| **`feat: 🗃️ Capa de datos con HashMap sobre MongoDB`** | `feature/data-layer` | Persistencia real, seed y búsquedas |
| **`feat: 🏁 Motor de clasificación con hilos`** | `feature/qualifying-engine` | Fórmula de tiempo de vuelta y `Task` |
| **`feat: 🎨 Shell de navegación y carga asíncrona`** | `feature/ui-shell` | Navegación real y arranque no bloqueante |
| **`feat: 🎮 Vistas CRUD, búsqueda y comparación`** | `feature/ui-catalogs` | Columnas reales, buscadores, comparador y ficha |
| **`feat: ⚡ Simulación con hilos e historial`** | `feature/ui-simulation` | Sesión completa y almacenamiento |
| **`docs: 📝 Realinear el README`** | `docs/realign` | Este documento |

## Pendiente

- **Diseño de Figma**: las vistas usan la paleta del proyecto pero aún no reproducen el diseño de Figma (no se pudo extraer por el límite del plan). Las vistas no llevan estilos en línea, así que adaptarlas será cambiar `style.css`.
- Imágenes de equipos, vehículos y trazados: los datos incluyen las URL pero la interfaz todavía no las muestra.
