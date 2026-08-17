# Estado de épicas, HU, RF y RNF

Este documento refleja el estado del proyecto después de implementar HU-36. La referencia histórica sigue siendo `docs/legacy/ProyectoFormula1.md`; el alcance operativo vigente está resumido en el README.

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
| E12 | Análisis automático de la sesión | **Terminada** |

## Historias de usuario

| Épica | Historias | Estado |
|---|---|---|
| E01 | HU-01 a HU-06 | **Terminadas** |
| E02 | HU-07 a HU-13 | **Terminadas** |
| E03 | HU-14 a HU-19 | **Terminadas** |
| E04 | HU-20 a HU-23 | **Terminadas** |
| E05 | HU-24 OpenF1 | **En desarrollo** |
| E05 | HU-25 a HU-27 | **Terminadas** |
| E06 | HU-28 | **Terminada** |
| E07 | HU-29 | **Terminada** |
| E08 | HU-30 | **Terminada** |
| E09 | HU-31 a HU-33 | **Terminadas** |
| E10 | HU-34 | **Terminada** |
| E11 | HU-35 | **Terminada** |
| E12 | HU-36 | **Terminada** |

## Totales

| Elemento | Terminadas | En desarrollo | Total |
|---|---:|---:|---:|
| Épicas | 11 | 1 | 12 |
| Historias de usuario | 35 | 1 | 36 |

La única HU pendiente frente al documento legacy es **HU-24 Consumir OpenF1**. OpenF1 está fuera del alcance operativo vigente del README, por eso la aplicación principal ya funciona completa sin depender de esa integración.

## Requisitos funcionales

| Requisito | Estado | Evidencia |
|---|---|---|
| RF-01 a RF-04 | **Cumplidos** | CRUD completo de equipos, pilotos, vehículos y circuitos con servicios, repositorios y controladores JavaFX. |
| RF-05 | **Cumplido** | Relaciones piloto-equipo-vehículo y validación de pertenencia antes de simular. |
| RF-06 | **Cumplido** | Selección de circuito, piloto y vehículo; combinaciones inválidas se rechazan. |
| RF-07 a RF-10 | **Cumplidos** | Configuración de conducción, aerodinámica, presión de neumáticos y combustible. |
| RF-11 | **Cumplido** | Clima inicial y evolución climática según circuito. |
| RF-12 | **Cumplido** | Rendimiento con piloto, vehículo, circuito, clima, configuración, eventos y pista. |
| RF-13 | **Cumplido** | Tiempo de vuelta probabilístico con semilla controlable en pruebas. |
| RF-14 | **Cumplido** | Ordenamiento, posiciones y diferencias con la pole. |
| RF-15 | **Cumplido** | Clasificación final visible en JavaFX. |
| RF-16 | **Cumplido** | Estadísticas, gráfico de métricas, sectores, clima, pista, eventos y análisis automático. |
| RF-17 | **En desarrollo** | Depende de OpenF1, fuera del alcance operativo vigente. |
| RF-18 | **Cumplido** | Persistencia duradera en MongoDB con respaldo en memoria. |
| RF-19 | **Cumplido** | Resultados, eventos, telemetría, clima, sectores, pista y análisis se guardan con la sesión. |
| RF-20 | **Cumplido** | Historial de sesiones y configuraciones reutilizables. |
| RF-21 | **En desarrollo** | Depende de OpenF1, fuera del alcance operativo vigente. |

Resumen RF: **19 cumplidos de 21**; los 2 restantes corresponden a OpenF1.

## Requisitos no funcionales generales

| Requisito | Estado | Evidencia |
|---|---|---|
| RNF-01 | **Cumplido** | Java 17 configurado en Maven y compilación verificada con `javac --release 17`. |
| RNF-02 | **Parcial** | UI JavaFX consistente; imágenes externas del diseño/escuderías no están integradas. |
| RNF-03 | **Cumplido** | MongoDB con Repository y modo memoria resiliente. |
| RNF-04 | **En desarrollo** | Consumo OpenF1 no implementado. |
| RNF-05 | **Cumplido** | Separación actual en `model`, `data`, `service`, `event`, `controller` y `util`. |
| RNF-06 | **Cumplido** | Packages cohesionados por responsabilidad. |
| RNF-07 | **Cumplido** | Validación en UI y servicios. |
| RNF-08 | **Parcial** | Errores de entrada y persistencia cubiertos; OpenF1 no aplica todavía. |
| RNF-09 | **Cumplido** | Carga y simulación en `Task`/pool de hilos, sin bloquear JavaFX. |
| RNF-10 | **Cumplido** | Flujo completo desde la interfaz, sin manejo manual de MongoDB. |
| RNF-11 | **Cumplido** | Dependencias declaradas en Maven. |
| RNF-12 | **Cumplido** | Repositorio versionado y documentación de cambios. |

Resumen RNF generales: **9 cumplidos, 2 parciales, 1 en desarrollo**.

## POO y calidad

| Requisitos | Estado | Evidencia |
|---|---|---|
| RNF-18 a RNF-23 | **Cumplidos** | Encapsulamiento, objetos de dominio, records inmutables, herencia de eventos y control de visibilidad. |
| RNF-24 | **Cumplido** | Estrategias de conducción/fuel/aerodinámica/neumáticos encapsulan comportamiento mediante enums con factores y reglas propias. |
| RNF-25 a RNF-28 | **Cumplidos** | Interfaces, sobrecarga útil, lambdas y Streams en búsquedas, métricas, análisis y callbacks. |
| RNF-29 a RNF-30 | **Cumplidos** | Concurrencia con pool, `Task` y actualización segura de JavaFX. |
| RNF-31 a RNF-34 | **Cumplidos** | Excepciones de validación/datos y ausencia de `System.exit` en flujo normal. |
| RNF-35 a RNF-43 | **Cumplidos** | Responsabilidades acotadas, imports explícitos, utilidades enfocadas y comentarios solo en decisiones no obvias. |

Resumen POO/calidad: **26 cumplidos de 26**.

## Controles de calidad aplicados

| Control | Estado |
|---|---|
| Compilación | Código principal y pruebas compilan con Java 17. |
| Pruebas automatizadas | **94 pruebas** ejecutadas correctamente. |
| Integridad FXML | `ViewsLoadTest` carga los **14 FXML**, incluida la pestaña HU-36. |
| Persistencia histórica | JSON compatible con sesiones antiguas sin telemetría, sectores, evolución de pista o análisis. |
| Inmutabilidad | Snapshots, eventos, sectores, evolución de pista y análisis son objetos inmutables o expuestos como copias. |
| Trazabilidad | Documentos por funcionalidad y matriz actualizada de épicas/HU/RF/RNF. |

## Observaciones de alcance

- HU-29 genera y persiste evolución climática; grip, tracción, frenado, temperaturas, consumo, desgaste y tiempo reaccionan a ella.
- HU-30 incorpora eventos ponderados con impactos reales sobre tiempo, pista, clima, telemetría y validez de vuelta.
- HU-32 persiste 20 muestras de vuelta del piloto seleccionado.
- HU-33 calcula S1, S2 y S3 para vueltas válidas e identifica mejores sectores.
- HU-34 acumula goma, limpia pista con lluvia y aplica el grip al motor antes de calcular cada vuelta.
- HU-36 genera análisis por reglas: pole, margen, sectores, consumo, desgaste, eventos, clima, evolución de pista y telemetría.
