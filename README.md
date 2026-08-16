<p align="center">
  <img src="docs/assets/logo.svg" width="640" alt="Formula1Simulator logo">
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-e10600?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="JavaFX" src="https://img.shields.io/badge/JavaFX-17.0.10-e10600?style=flat-square&logo=java&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-build-e10600?style=flat-square&logo=apachemaven&logoColor=white">
  <img alt="MongoDB" src="https://img.shields.io/badge/MongoDB-5.1.1-e10600?style=flat-square&logo=mongodb&logoColor=white">
  <img alt="Estado" src="https://img.shields.io/badge/Estado-MVP%20Nivel%201%20(estructura)-e10600?style=flat-square">
</p>

# Formula1Simulator

Aplicación de escritorio en **Java 17 + JavaFX** para simular una sesión de **clasificación de Fórmula 1**: gestión de equipos, pilotos, vehículos y circuitos, configuración de la simulación, y ejecución de una clasificación cuyo resultado depende del piloto, el vehículo, el circuito, el clima y la estrategia elegida. Persistencia en **MongoDB** y datos reales vía **OpenF1 API**.

La especificación completa (historias de usuario, requisitos funcionales/no funcionales y arquitectura objetivo) vive en [`ProyectoFormula1.md`](ProyectoFormula1.md).

---

## Tabla de contenidos

- [Estado del proyecto](#estado-del-proyecto)
- [Tecnologías](#tecnologías)
- [Arquitectura y patrones de diseño](#arquitectura-y-patrones-de-diseño)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos previos](#requisitos-previos)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Flujo de trabajo Git](#flujo-de-trabajo-git)
- [Historial de cambios](#historial-de-cambios)
- [Documentación adicional](#documentación-adicional)
- [Próximos pasos](#próximos-pasos)

---

## Estado del proyecto

Se completó la **estructura del núcleo MVP (Nivel 1)** descrito en `ProyectoFormula1.md`: todas las capas, paquetes y patrones de diseño están creados y el proyecto compila y arranca de punta a punta. La lógica de negocio pesada se dejó deliberadamente como placeholders ruidosos (`UnsupportedOperationException("TODO: ...")`) en vez de datos falsos silenciosos.

| Funciona hoy | Pendiente (lanza `UnsupportedOperationException` a propósito) |
|---|---|
| ✅ Compila limpio (`mvn compile`) y la app JavaFX arranca (`mvn javafx:run`) | ⛔ Persistencia real en MongoDB (mapeo entidad ↔ `Document`) |
| ✅ Modelo de dominio completo (getters/setters/equals/toString) | ⛔ Consumo real de la API de OpenF1 |
| ✅ Validaciones de negocio en la capa `service` | ⛔ Motor de simulación (`LapCalculator`, `PerformanceCalculator`, `SimulationEngine`) |
| ✅ `WeatherService` genera clima inicial real | ⛔ Formularios de alta/baja (handlers de UI vacíos) |
| ✅ Tablas de la UI cargan sin romperse (vacías, de forma segura) | ⛔ Navegación entre vistas del dashboard |

## Tecnologías

- **Java 17**
- **JavaFX** 17.0.10 (controls + fxml)
- **MongoDB** (driver síncrono 5.1.1)
- **OpenF1 API** (vía `java.net.http.HttpClient`)
- **Jackson** 2.17.2 (databind + JSR-310)
- **Maven**
- **JUnit 5** (5.10.3)

## Arquitectura y patrones de diseño

El proyecto separa responsabilidades en capas (`model → repository/api → service → simulation → controller → UI`) y aplica los patrones exigidos por la especificación en el alcance ya implementado:

| Patrón | Dónde | Propósito |
|---|---|---|
| **Repository** | `repository.CrudRepository<T, ID>` + implementaciones | Aísla el acceso a MongoDB del resto del sistema |
| **Adapter** | `api.F1DataProvider` / `api.OpenF1ApiAdapter` | Desacopla el formato JSON de OpenF1 del dominio |
| **Facade** | `simulation.SimulationFacade` | Punto único de entrada para iniciar/consultar una clasificación |
| **Singleton** | `database.MongoConnection` | Una única conexión compartida a MongoDB |

Los patrones **Strategy**, **Observer** y **Factory** (conducción dinámica, telemetría, eventos aleatorios) pertenecen al Nivel 2 del `.md` y quedan fuera de este alcance — ver [Próximos pasos](#próximos-pasos).

## Estructura del proyecto

```text
Formula1Simulator/
├── ProyectoFormula1.md          # Especificación completa (HU, RF, RNF, arquitectura)
├── README.md
├── docs/
│   ├── assets/logo.svg
│   └── features/                # Un .md por feature implementada (qué se hizo y qué falta)
└── simulator/
    ├── pom.xml
    └── src/main/
        ├── java/com/formula1/
        │   ├── App.java                  # javafx.application.Application
        │   ├── Main.java                 # Launcher (App.launch)
        │   ├── model/                    # Driver, Team, Vehicle, Circuit, Weather,
        │   │                             # SimulationConfig, Simulation, Result + enums
        │   ├── exception/                # Excepciones de dominio (RNF-33)
        │   ├── util/                     # DateUtils, ValidationUtils, FormatUtils,
        │   │                             # MathUtils, RandomUtils
        │   ├── database/                 # MongoConnection (Singleton)
        │   ├── repository/               # CrudRepository<T,ID> + repos por entidad
        │   ├── api/                      # F1DataProvider (Adapter) + OpenF1ApiAdapter
        │   ├── service/                  # Servicios de negocio (Driver/Team/Vehicle/
        │   │                             # Circuit/Weather/OpenF1/Simulation)
        │   ├── simulation/               # SimulationEngine, LapCalculator,
        │   │                             # PerformanceCalculator, SimulationFacade
        │   └── controller/               # Controllers JavaFX (Dashboard, Driver, Team,
        │                                 # Vehicle, Circuit, Simulation)
        └── resources/
            ├── views/                    # dashboard.fxml, drivers.fxml, teams.fxml,
            │                             # vehicles.fxml, circuits.fxml, simulation.fxml
            └── css/style.css
```

## Requisitos previos

- JDK 17
- Maven 3.8+
- (Opcional para persistencia real) una instancia de MongoDB accesible — por defecto `mongodb://localhost:27017`, configurable con las variables de entorno `MONGO_URI` y `MONGO_DATABASE`

## Instalación y ejecución

```bash
cd simulator

# Compilar
mvn compile

# Ejecutar la aplicación JavaFX
mvn javafx:run

# Ejecutar tests
mvn test
```

## Flujo de trabajo Git

- **`develop`** es la rama de integración (antes `developer`).
- Cada pieza de la estructura se implementó en su propia rama **`feature/<nombre>`**, con su commit siguiendo **Conventional Commits + gitmoji** (`tipo: :emoji: descripción`) y mergeada a `develop` con `--no-ff`.
- Cada `feature/*` documenta lo implementado en [`docs/features/<nombre>.md`](docs/features).

## Historial de cambios

Commits aplicados hasta ahora (orden cronológico, `feat: :emoji: descripción`):

| Commit | Rama | Descripción |
|---|---|---|
| `feat: 🎉 Structure project_initializate` | `developer` | Inicialización del repositorio |
| `feat: 🚧 Creation branch developer` | `developer` | Creación de la rama de desarrollo |
| `feat: 📦 Configurar dependencias y plugins de Maven` | `feature/pom-setup` | JavaFX, MongoDB, Jackson, JUnit + plugins; `.gitignore` |
| `feat: 🧱 Crear modelo de dominio` | `feature/model` | `Driver`, `Team`, `Vehicle`, `Circuit`, `Simulation`, `Result`, `Weather` + enums |
| `feat: 🔧 Crear excepciones de dominio y utilidades transversales` | `feature/exceptions-util` | Paquetes `exception/` y `util/` |
| `feat: 🗃️ Crear conexión Mongo y repositorios CRUD` | `feature/database-repository` | `MongoConnection` (Singleton) + `repository/` (Repository) |
| `feat: 🛰️ Crear adapter de integración con OpenF1` | `feature/api-openf1` | `F1DataProvider` + `OpenF1ApiAdapter` (Adapter) |
| `feat: ⚙️ Crear capa de servicios` | `feature/service` | Servicios de negocio + validaciones |
| `feat: 🏁 Crear motor de simulación y facade` | `feature/simulation-engine` | `SimulationEngine`, calculadores, `SimulationFacade` (Facade) |
| `feat: 🎮 Crear controllers de JavaFX` | `feature/controllers` | Controllers de todas las vistas |
| `feat: 🎨 Crear bootstrap de JavaFX y vistas placeholder` | `feature/javafx-bootstrap` | `App`/`Main`, FXML, CSS — verificado con `mvn javafx:run` |

## Documentación adicional

- [`ProyectoFormula1.md`](ProyectoFormula1.md) — especificación completa del proyecto.
- [`docs/features/`](docs/features) — un `.md` por cada feature, con las clases creadas, el patrón de diseño aplicado y lo que queda pendiente.

## Próximos pasos

Trabajo pendiente para que el simulador sea funcional de punta a punta:

1. Mapeo real entidad ↔ `Document` en los `*RepositoryImpl` (persistencia MongoDB).
2. Consumo real de la API de OpenF1 en `OpenF1ApiAdapter`.
3. Fórmula de rendimiento y tiempo de vuelta en `LapCalculator` / `PerformanceCalculator` / `SimulationEngine`.
4. Formularios de alta/baja/edición en los controllers y navegación entre vistas.
5. **Nivel 2 (MVP diferencial)**: motor probabilístico, clima dinámico, eventos aleatorios y telemetría visual — introducen los patrones **Strategy**, **Observer** y **Factory** en los paquetes `strategy/`, `event/` y `telemetry/`.
