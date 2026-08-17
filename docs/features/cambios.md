# Estado de épicas e historias de usuario

Este documento registra el estado actual del proyecto frente a las épicas e historias definidas en `docs/legacy/ProyectoFormula1.md`.

Estados utilizados:

- **Terminada:** la funcionalidad solicitada está implementada en el proyecto actual.
- **En desarrollo:** la funcionalidad está parcial o todavía no está implementada. Se agrupan ambos casos porque se solicitaron únicamente estos dos estados.

Una épica se considera **Terminada** solamente cuando todas sus historias están terminadas.

## Resumen de épicas

| Épica | Nombre | Estado |
|---|---|---|
| E01 | Gestión de datos de Fórmula 1 | **Terminada** |
| E02 | Configuración de la simulación | **Terminada** |
| E03 | Motor de simulación | **Terminada** |
| E04 | Visualización de resultados | **Terminada** |
| E05 | Integración y persistencia | **En desarrollo** |
| E06 | Motor probabilístico de simulación | **Terminada** |
| E07 | Clima dinámico | **Terminada** |
| E08 | Sistema de eventos aleatorios | **Terminada** |
| E09 | Telemetría visual | **Terminada** |
| E10 | Evolución dinámica de pista | **Terminada** |
| E11 | Sistema de estrategia | **Terminada** |
| E12 | Análisis automático de la sesión | **En desarrollo** |

## E01 — Gestión de datos de Fórmula 1

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-01 | Gestionar equipos | **Terminada** |
| HU-02 | Gestionar pilotos | **Terminada** |
| HU-03 | Gestionar vehículos | **Terminada** |
| HU-04 | Gestionar circuitos | **Terminada** |
| HU-05 | Consultar información | **Terminada** |
| HU-06 | Comparar vehículos | **Terminada** |

## E02 — Configuración de la simulación

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-07 | Seleccionar circuito | **Terminada** |
| HU-08 | Seleccionar piloto y vehículo | **Terminada** |
| HU-09 | Configurar conducción | **Terminada** |
| HU-10 | Configurar aerodinámica | **Terminada** |
| HU-11 | Configurar neumáticos | **Terminada** |
| HU-12 | Configurar combustible | **Terminada** |
| HU-13 | Guardar configuración | **Terminada** |

## E03 — Motor de simulación

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-14 | Obtener condiciones climáticas | **Terminada** |
| HU-15 | Calcular rendimiento | **Terminada** |
| HU-16 | Calcular tiempo de vuelta | **Terminada** |
| HU-17 | Ejecutar clasificación | **Terminada** |
| HU-18 | Generar clasificación | **Terminada** |
| HU-19 | Mostrar evolución | **Terminada** |

## E04 — Visualización de resultados

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-20 | Visualizar clasificación | **Terminada** |
| HU-21 | Visualizar estadísticas | **Terminada** |
| HU-22 | Visualizar clima | **Terminada** |
| HU-23 | Visualizar telemetría | **Terminada** |

## E05 — Integración y persistencia

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-24 | Consumir OpenF1 | **En desarrollo** |
| HU-25 | Guardar datos | **Terminada** |
| HU-26 | Guardar resultados | **Terminada** |
| HU-27 | Consultar historial | **Terminada** |

## E06 — Motor probabilístico de simulación

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-28 | Simulación probabilística | **Terminada** |

## E07 — Clima dinámico

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-29 | Evolución climática | **Terminada** |

## E08 — Sistema de eventos aleatorios

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-30 | Generar eventos | **Terminada** |

## E09 — Telemetría visual

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-31 | Visualizar telemetría | **Terminada** |
| HU-32 | Visualizar evolución de la vuelta | **Terminada** |
| HU-33 | Comparar sectores | **Terminada** |

## E10 — Evolución dinámica de pista

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-34 | Evolución del grip | **Terminada** |

## E11 — Sistema de estrategia

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-35 | Seleccionar estrategia | **Terminada** |

## E12 — Análisis automático de la sesión

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-36 | Generar análisis | **En desarrollo** |

## Totales

| Elemento | Terminadas | En desarrollo | Total |
|---|---:|---:|---:|
| Épicas | 8 | 4 | 12 |
| Historias de usuario | 32 | 4 | 36 |

## Auditoría de requisitos cumplidos

Esta auditoría corresponde al estado del proyecto después de HU-30. Un requisito se marca como cumplido solamente cuando existe implementación verificable; una declaración en el README no se considera evidencia suficiente por sí sola.

### Resumen

| Grupo | Cumplidos | Definidos en `ProyectoFormula1.md` |
|---|---:|---:|
| Requisitos funcionales | 19 | 21 |
| RNF generales | 8 | 12 |
| RNF de POO y calidad | 25 | 26 |
| **Total RNF** | **33** | **38** |

### Requisitos funcionales cumplidos

| Requisitos | Cumplimiento verificado |
|---|---|
| RF-01 a RF-04 | CRUD completo de equipos, pilotos, vehículos y circuitos mediante controladores, servicios y repositorios. |
| RF-05 | Relaciones entre pilotos, equipos y vehículos, con validación de pertenencia a la escudería. |
| RF-06 | Selección de circuito, piloto y vehículo; se rechazan combinaciones piloto/vehículo inválidas. |
| RF-07 a RF-10 | Configuración de conducción, aerodinámica, presión de neumáticos y combustible. |
| RF-11 | Generación de clima según la distribución configurada para el circuito. |
| RF-12 | Cálculo de rendimiento considerando piloto, vehículo, circuito, clima y configuración. |
| RF-13 | Cálculo probabilístico del tiempo de vuelta. |
| RF-14 | Ordenamiento de participantes por tiempo y cálculo de posiciones y diferencias. |
| RF-15 | Presentación de la clasificación final en JavaFX. |
| RF-16 | Resumen estadístico y gráfico de barras para comparar tiempos, diferencias, consumo y desgaste. |
| RF-18 | Persistencia de entidades y sesiones en MongoDB, con operación alternativa en memoria. |
| RF-19 | Almacenamiento de resultados de clasificación. |
| RF-20 | Consulta de sesiones almacenadas, parrillas y configuraciones anteriores. |

RF-17 y RF-21 dependen de OpenF1, integración que fue retirada del alcance vigente y no está implementada.

### Requisitos no funcionales generales cumplidos

| Requisito | Cumplimiento verificado |
|---|---|
| RNF-01 | Código compilado para Java 17 mediante la propiedad `maven.compiler.release`. |
| RNF-03 | Persistencia duradera implementada con el driver de MongoDB y el patrón Repository. |
| RNF-06 | Organización por responsabilidades en `model`, `data`, `service`, `controller` y `util`. |
| RNF-07 | Validaciones previas al guardado y antes de ejecutar la simulación. |
| RNF-09 | Carga, simulación y operaciones de persistencia fuera del hilo principal de JavaFX. |
| RNF-10 | Flujo completo de configuración y simulación desde la interfaz, sin acceso manual a MongoDB. |
| RNF-11 | Dependencias y compilación reproducibles mediante Maven y Java 17. |
| RNF-12 | Repositorio Git conectado a GitHub y commits descriptivos con convenciones del equipo. |

RNF-02 está cumplido en JavaFX, pero no completamente en el uso de imágenes SVG dentro de la aplicación. RNF-04 depende de OpenF1. RNF-05 mantiene separación de responsabilidades, pero no posee las capas `api` y `simulation` independientes descritas en el documento. RNF-08 cubre errores de entrada y persistencia, pero no errores de OpenF1.

### Requisitos de POO y calidad cumplidos

| Requisitos | Cumplimiento verificado |
|---|---|
| RNF-18 | POO con encapsulamiento, abstracciones, herencia lógica de JavaFX y polimorfismo mediante interfaces. |
| RNF-19 y RNF-20 | Atributos encapsulados, getters/setters controlados y uso justificado de `this`. |
| RNF-21 | Las entidades principales están modeladas: piloto, equipo, vehículo, circuito, sesión, resultado, clima, telemetría, evento y estrategia. |
| RNF-22 y RNF-23 | Visibilidad mínima necesaria y herencia lógica entre el evento abstracto y sus especializaciones de rendimiento y pista. |
| RNF-25 y RNF-26 | Sobrecarga útil e interfaces como `CrudRepository`, `Progreso` y `Evolucion`. |
| RNF-27 y RNF-28 | Lambdas y Streams empleados en filtros, búsquedas, ordenamiento, callbacks y agrupaciones. |
| RNF-29 y RNF-30 | Pool de hilos, `Task` de JavaFX y actualizaciones visuales mediante el JavaFX Application Thread. |
| RNF-31 a RNF-33 | Excepciones para datos, configuración y persistencia; `ValidationException` y `DataAccessException`. |
| RNF-34 | No se utiliza `System.exit` para controlar el flujo de la aplicación. |
| RNF-35 a RNF-39 | Packages cohesionados, utilidades enfocadas, imports explícitos, separación de capas y responsabilidad principal por clase. |
| RNF-40 a RNF-43 | Comentarios sobre decisiones no evidentes, métodos enfocados, comportamiento dentro de objetos y lógica reutilizable. |

RNF-24 no está completo: los modos de conducción funcionan, pero todavía no están modelados como implementaciones polimórficas de una abstracción `DrivingStrategy`.

## Controles de calidad aplicados

| Control | Estado y evidencia |
|---|---|
| Compilación | Código principal y pruebas compilan con Java 17. |
| Pruebas automatizadas | 88 pruebas ejecutadas correctamente, incluida una regresión de 250 carreras consecutivas. |
| Integridad de vistas | `ViewsLoadTest` carga los trece archivos FXML y detecta IDs, acciones o imports inválidos. |
| Pruebas del motor | Fórmula, clima dinámico, ordenamiento, configuración, HU-08, HU-19, estadísticas de HU-21, telemetría de HU-23, eventos de HU-30, evolución de HU-32, comparación de HU-33 y pista dinámica de HU-34 están cubiertos. |
| Validación en capas | La interfaz previene entradas incompletas y los servicios protegen nuevamente las reglas de negocio. |
| Inmutabilidad e invariantes | `SimulationSnapshot`, `TelemetrySnapshot`, `WeatherSnapshot`, `EventOccurrence`, `EventImpact`, `SectorTimes` y `TrackEvolutionSnapshot` son inmutables; la sesión tampoco expone listas mutables de evolución. |
| Seguridad entre hilos | `Task`, un pool compartido y `Platform.runLater` evitan bloquear o actualizar JavaFX desde un hilo incorrecto. |
| Persistencia resiliente | MongoDB funciona como almacenamiento duradero y `ConcurrentHashMap` permite continuar en modo memoria. |
| Patrones exigidos por el alcance vigente | Repository para persistencia y Singleton para conexión y almacén compartido. |
| Reproducibilidad | Semilla opcional del motor de eventos, semillas fijas en pruebas y formatos técnicos independientes de la configuración regional. |
| Trazabilidad | Documentos por funcionalidad, matriz de épicas/HU y commits descriptivos en Git. |

## Observaciones de alcance

- El alcance vigente descrito por el README es `f1project.md`; `ProyectoFormula1.md` se conserva dentro de `docs/legacy` y contiene funcionalidades adicionales.
- OpenF1 y Q1/Q2/Q3 continúan fuera del alcance vigente. La telemetría se reincorporó como extensión mediante HU-23.
- HU-29 genera y persiste 20 estados climáticos por sesión; grip, tracción, frenado, temperaturas, consumo, desgaste y tiempo reaccionan a ellos.
- HU-30 incorpora 28 eventos + `NO_EVENT`, probabilidades configurables, selección ponderada contextual, cooldown, sectores y accidentes con vuelta invalidada, posible abandono y banderas.
- Las etiquetas visibles de HU-30 están localizadas al español para eventos, categorías y alcance; los identificadores internos permanecen estables para no romper datos guardados.
- HU-23 también satisface HU-31 porque implementa su dashboard detallado en tiempo real.
- HU-32 grafica y persiste 20 muestras de velocidad, tiempo, desgaste, combustible, temperaturas y delta.
- HU-33 calcula y persiste S1, S2 y S3 para cada vuelta válida, compara toda la parrilla e identifica automáticamente al piloto más rápido de cada sector.
- HU-34 acumula goma en seco, la elimina con lluvia y aplica el grip resultante al motor antes de calcular cada vuelta; la evolución completa queda persistida y visible.
