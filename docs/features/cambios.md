# Estado de épicas, HU, RF y RNF

Este documento separa el estado implementado del nuevo backlog de trabajo. La
persistencia ya está terminada; las épicas E13 a E22 se incorporan como planificación
pendiente para desarrollarlas progresivamente. La referencia histórica sigue siendo
`docs/legacy/ProyectoFormula1.md` y el alcance operativo actual está resumido en el
README.

Una HU permanece **pendiente** hasta completar todos sus criterios, aunque el proyecto
ya contenga pantallas, datos o servicios que puedan reutilizarse como punto de partida.

## Resumen de épicas

| Épica | Nombre | Estado |
|---|---|---|
| E01 | Gestión de datos de Fórmula 1 | **Terminada** |
| E02 | Configuración de la simulación | **Terminada** |
| E03 | Motor de simulación | **Terminada** |
| E04 | Visualización de resultados | **Terminada** |
| E05 | Persistencia de datos | **Terminada** |
| E06 | Motor probabilístico de simulación | **Terminada** |
| E07 | Clima dinámico | **Terminada** · integrado en Carrera |
| E08 | Sistema de eventos aleatorios | **Terminada** |
| E09 | Telemetría visual | **Terminada** |
| E10 | Evolución dinámica de pista | **Terminada** · integrada en Carrera |
| E11 | Sistema de estrategia | **Terminada** |
| E13 | Rediseño de experiencia de Carrera | **Terminada** |
| E14 | Información detallada bajo demanda | **Pendiente** |
| E15 | Simplificación del módulo Carrera | **Pendiente** |
| E16 | Rediseño del módulo Explorar | **Pendiente** |
| E17 | Seguimiento dinámico del piloto | **Pendiente** |
| E18 | Estrategia y Pit Stop | **Pendiente** |
| E19 | Resultados de clasificación | **Pendiente** |
| E20 | Recursos multimedia | **Cancelada** |
| E21 | Arquitectura y patrones de diseño | **Pendiente** |
| E22 | Animación inicial | **Terminada** · opcional |

## Historias de usuario

| Épica | Historias | Estado |
|---|---|---|
| E01 | HU-01 a HU-06 | **Terminadas** |
| E02 | HU-07 a HU-13 | **Terminadas** |
| E03 | HU-14 a HU-19 | **Terminadas** |
| E04 | HU-20 a HU-23 | **Terminadas** |
| E05 | HU-25 a HU-27 | **Terminadas** |
| E06 | HU-28 | **Terminada** |
| E07 | HU-29 | **Terminada** · sin pestaña independiente |
| E08 | HU-30 | **Terminada** |
| E09 | HU-31 a HU-33 | **Terminadas** |
| E10 | HU-34 | **Terminada** · sin pestaña independiente |
| E11 | HU-35 | **Terminada** |
| E13 | HU-37 a HU-40 | **Terminadas** |
| E14 | HU-41 y HU-42 | **Terminadas** |
| E15 | HU-43 | **Pendiente** |
| E16 | HU-44 | **Terminada** |
| E16 a HU-47 | **Pendientes** |
| E17 | HU-48 y HU-49 | **Pendientes** |
| E18 | HU-50 a HU-52 | **Pendientes** |
| E19 | HU-53 | **Pendiente** |
| E20 | HU-54 | **Cancelada** |
| E21 | HU-55 y HU-56 | **Pendientes** |
| E22 | HU-57 | **Terminada** · opcional |

## Totales del alcance vigente

| Elemento | Terminadas | Pendientes | Total vigente |
|---|---:|---:|---:|
| Épicas | 12 | 9 | 21 |
| Historias de usuario | 38 | 17 | 55 |

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
| RF-16 | **Cumplido** | Clasificación, telemetría en vivo, sectores y eventos; grip y clima se resumen en Carrera. |
| RF-18 | **Cumplido** | Persistencia duradera en MongoDB con respaldo en memoria. |
| RF-19 | **Cumplido** | Resultados, eventos, telemetría, clima, sectores y evolución de pista se guardan con la sesión. |
| RF-20 | **Cumplido** | Historial de sesiones y configuraciones reutilizables. |

Resumen RF: **19 cumplidos de 19 requisitos vigentes**.

## Requisitos no funcionales generales

| Requisito | Estado | Evidencia |
|---|---|---|
| RNF-01 | **Cumplido** | Java 17 configurado en Maven. |
| RNF-02 | **Cumplido** | Interfaz JavaFX consistente con imágenes de pilotos, vehículos, circuitos, escuderías y recursos del menú empaquetados. |
| RNF-03 | **Cumplido** | MongoDB con Repository y modo memoria resiliente. |
| RNF-05 | **Cumplido** | Separación en `model`, `data`, `service`, `event`, `controller` y `util`. |
| RNF-06 | **Cumplido** | Packages cohesionados por responsabilidad. |
| RNF-07 | **Cumplido** | Validación en interfaz y servicios. |
| RNF-08 | **Cumplido** | Errores de entrada, carga multimedia y persistencia tratados sin cerrar la aplicación. |
| RNF-09 | **Cumplido** | Carga y simulación en `Task`/pool de hilos, sin bloquear JavaFX. |
| RNF-10 | **Cumplido** | Flujo completo desde la interfaz, sin manejo manual de MongoDB. |
| RNF-11 | **Cumplido** | Dependencias declaradas en Maven. |
| RNF-12 | **Cumplido** | Repositorio versionado y documentación de cambios. |

Resumen RNF generales: **11 cumplidos de 11 requisitos vigentes**.

## POO y calidad

| Requisitos | Estado | Evidencia |
|---|---|---|
| RNF-18 a RNF-23 | **Cumplidos** | Encapsulamiento, objetos de dominio, records inmutables, herencia de eventos y control de visibilidad. |
| RNF-24 | **Cumplido** | Estrategias de conducción, combustible, aerodinámica y presión encapsulan factores y reglas propias. |
| RNF-25 a RNF-28 | **Cumplidos** | Interfaces, sobrecarga útil, lambdas y Streams en búsquedas, métricas y callbacks. |
| RNF-29 a RNF-30 | **Cumplidos** | Concurrencia con pool, `Task` y actualización segura de JavaFX. |
| RNF-31 a RNF-34 | **Cumplidos** | Excepciones de validación/datos y ausencia de `System.exit` en el flujo normal. |
| RNF-35 a RNF-43 | **Cumplidos** | Responsabilidades acotadas, imports explícitos, utilidades enfocadas y comentarios solo en decisiones no obvias. |

Resumen POO/calidad: **26 cumplidos de 26**.

## Backlog técnico y seguimiento

### E13 — Rediseño de experiencia de Carrera

> Es la épica de mayor prioridad dentro del nuevo alcance.

#### HU-37 — Rediseñar Dashboard de Carrera — **Terminada**

- Rediseñar el Dashboard.
- Integrar clasificación, evolución, información del vehículo, circuito y clima básico.
- Integrar la selección desde la clasificación.
- Crear una representación visual del circuito.
- Mostrar los pilotos mediante círculos sobre el circuito.
- Actualizar visualmente la posición de los pilotos.
- Identificar cada piloto y diferenciar el piloto fijado.

El trazado ya no es un PNG: se dibuja con una spline Catmull-Rom centrípeta reparametrizada por
longitud de arco, calcada de los mapas oficiales por `tools/TrazarCircuitos.java`. Los veinte
pilotos lo recorren en vivo con el color de su escudería, y su posición se deriva del **mismo
`List<LapResult>` que puebla la torre de tiempos**, de modo que el orden en pista no puede
contradecir al de la tabla —hay una prueba que lo fija—. Detalle en
[`circuito-dinamico.md`](circuito-dinamico.md).

> El «piloto fijado» que menciona el último punto es HU-48. Aquí queda el gancho: el mapa ya
> distingue al líder y al piloto del usuario, y resalta en los dos sentidos entre tabla y pista.

#### HU-38 — Integrar clima básico — **Terminada**

- Mostrar temperatura, humedad, lluvia y condición actual.
- Actualizar el clima dinámicamente.
- Integrarlo visualmente con el Dashboard.

#### HU-39 — Integrar comparación dinámica de sectores — **Terminada**

- Mostrar S1, S2 y S3.
- Identificar el mejor sector y la diferencia entre pilotos.
- Integrar la comparación con la clasificación.
- Actualizarla según el piloto seleccionado.

#### HU-40 — Configurar duración de simulación — **Terminada**

- Incorporar un selector con 5, 10, 30 y 60 segundos, 2 minutos y duración personalizada.
- Permitir la finalización manual.
- Mostrar el contador y la duración seleccionada.
- Detener la simulación al alcanzar el límite.
- Guardar los resultados generados.

> La duración debe ser una configuración real del motor, no un `Thread.sleep()` más
> largo en JavaFX.

La duración forma parte de `SimulationConfig` y el motor la distribuye entre
los segmentos mediante un regulador independiente de JavaFX. La finalización
manual conserva y guarda la última clasificación, telemetría, clima y eventos
que ya fueron emitidos.

### E14 — Información detallada bajo demanda **Terminada**

#### HU-41 — Consultar telemetría detallada **Terminada**

- Añadir una acción «Ver detalles».
- Mostrar gráficas de velocidad, RPM, combustible, desgaste, temperaturas y delta.
- Permitir elegir la vuelta consultada.
- Permitir regresar al Dashboard.

#### HU-42 — Consultar eventos detallados **Terminada**

- Mostrar notificaciones pequeñas durante la simulación.
- Incluir tipo de evento, piloto afectado, sector e impacto.
- Conservar un historial de eventos.
- Ofrecer una vista detallada bajo demanda.

### E15 — Simplificación del módulo Carrera **Terminada**

#### HU-43 — Eliminar y reorganizar secciones de Carrera **Terminada**

Se eliminarán como secciones independientes Estadísticas, Evolución de vuelta,
Evolución de pista, Análisis y Clima dinámico.

- Eliminar pestañas, botones y rutas de navegación innecesarias.
- Mantener los datos necesarios para resultados.
- Verificar la compatibilidad de las sesiones históricas.
- Actualizar FXML, controladores y navegación.

### E16 — Rediseño del módulo Explorar

#### HU-44 — Rediseñar vista principal de pilotos ** Terminada**

- Crear una nueva vista basada en tarjetas.
- Mostrar imagen, nombre, equipo e información principal.
- Permitir seleccionar el piloto y acceder a su detalle.

#### HU-45 — Rediseñar detalle de pilotos

- Mostrar foto, nombre, equipo, palmarés, habilidades y rendimiento histórico.
- Añadir información adicional y navegación de regreso.

> El proyecto ya dispone de fichas con parte de esta información; la HU comprende su
> rediseño e integración dentro del nuevo flujo.

#### HU-46 — Rediseñar detalle de teams 

- Incorporar una galería de imágenes.
- Mostrar estadísticas de velocidad y aceleración.
- Mostrar características técnicas e información del equipo.
- Añadir navegación de regreso.

#### HU-47 — Rediseñar circuitos **Terminada**

- Crear nuevas tarjetas, paleta y diseño dinámico.
- Mostrar imagen o trazado, nombre, ubicación, longitud y vueltas.
- Permitir acceder al detalle.

### E17 — Seguimiento dinámico del piloto

#### HU-48 — Fijar piloto durante la simulación

- Seleccionar y fijar visualmente un piloto.
- Mantener el piloto fijado aunque se consulte otro.
- Permitir cambiarlo y mostrar un indicador visual.
- Mantenerlo fijado al finalizar.

> Piloto seleccionado y piloto fijado son estados distintos. El usuario puede consultar
> a un piloto mientras mantiene a otro fijado en el Dashboard.

#### HU-49 — Mostrar Radio/Box del piloto

- Al momento de integrar la hu49 pullear con la rama feature/conde para integrar la parada de boxes en la radio.
- Mostrar la radio del piloto fijado.
- Generar mensajes contextuales sobre posición, vuelta, vehículo y eventos.
- Informar entradas y salidas de boxes, cambios de neumáticos e incidentes.
- Mostrar información de estrategia y actualizarla en vivo. +++++++++++++++++++++++++++++++++

### E18 — Estrategia y Pit Stop

#### HU-50 — Implementar parada en boxes

- Implementar entrada, estado visual o animación, tiempo de parada y salida de boxes.
- Actualizar la posición tras la parada.
- Crear el evento de pit stop y su registro histórico.
- Integrarlo con la radio.

#### HU-51 — Implementar cambio de neumáticos

Compuestos contemplados: **S** (Soft), **M** (Medium) y **H** (Hard).

- Mantener el estado del neumático actual.
- Permitir todos los cambios entre S, M y H.
- Aplicar el impacto del compuesto al rendimiento.
- Registrar y visualizar cada cambio.
- Integrarlo con el pit stop.

#### HU-52 — Visualizar estrategia de neumáticos

- Mostrar el neumático actual en el dashboard donde estan los pilotos
- el historial de compuestos.
- Mostrar número de paradas y vueltas con cada compuesto.
- Mostrar la estrategia del piloto fijado y los eventos de cambio.
- Integrar la información en Radio/Box y en la vista posterior a la clasificación.

### E19 — Resultados de clasificación

#### HU-53 — Crear sección posterior a la clasificación

Al terminar la simulación se mostrarán:

- Clasificación final, piloto fijado, tiempo, gap y sectores.
- Neumáticos, paradas, eventos y radio relevante.
- Estadísticas, vehículo y configuración utilizada.
- Información general de la sesión.

Flujo objetivo: `Configuración` → `Simulación` → `Finalización` → `Resultados`.

### E20 — Recursos multimedia

#### HU-54 — Implementar vídeos de las pistas **Cancelada**

- Asociar un vídeo a cada circuito.
- Incorporar reproductor, reproducción, pausa y controles.
- Crear una pantalla de vídeo.
- Mostrar un estado alternativo si no existe vídeo.
- Manejar errores sin bloquear la aplicación.

> Requisito solicitado por Mayorga.

### E21 — Arquitectura y patrones de diseño

#### HU-55 — Implementar arquitectura hexagonal

- Evitar que el dominio dependa de JavaFX o MongoDB.
- Evitar que la aplicación dependa directamente de infraestructura.
- Definir un puerto de persistencia implementado por MongoDB.
- Hacer que JavaFX consuma casos de uso.
- Orientar las dependencias hacia el dominio.

#### HU-56 — Revisar e implementar patrones de diseño

El README documenta actualmente Repository, Singleton, Strategy, Observer y Factory.

- Identificar y revisar los patrones existentes.
- Determinar cuáles aportan valor real.
- Seleccionar los dos patrones principales que se defenderán en el proyecto.
- Documentar nombre, problema, solución, ubicación y clases involucradas.
- Refactorizar patrones mal aplicados.
- Evitar patrones incorporados únicamente para cumplir un requisito.

### E22 — Animación inicial (opcional)

#### HU-57 — Implementar animación inicial **Terminada**

- Crear una pantalla inicial con el logo del proyecto F1.
- Añadir una animación de duración configurable.
- Permitir omitirla.
- Implementar la transición al Dashboard.
- Manejar errores sin bloquear la carga.

Prioridad: **opcional / baja**.

## Backlog priorizado

| Fase / Sprint | Prioridad | Descripción | Historias de usuario |
|---|---|---|---|
| Sprint / Fase 1 | Alta | Núcleo de la nueva experiencia | ~~HU-37~~, HU-48, HU-49, HU-50, HU-51 |
| Sprint / Fase 2 | Alta | Integración visual pendiente | HU-43 |
| Sprint / Fase 3 | Media | Explorar | HU-44, HU-45, HU-46, HU-47 |
| Sprint / Fase 4 | Media | Resultado y multimedia | HU-52, HU-53, HU-54 |
| Sprint / Fase 5 | Baja | Arquitectura | HU-55, HU-56 |
| Opcional | Baja | Extras y flujo de interfaz | HU-57 |

## Persistencia de datos

La persistencia está **terminada**. `DataStore` mantiene mapas concurrentes como fuente
de verdad durante la ejecución y realiza escrituras *write-through* mediante
`MongoRepository`. Al arrancar intenta recuperar MongoDB; si no está disponible, carga
el seed y conserva un modo memoria operativo.

Se persisten pilotos, equipos, vehículos, circuitos y sesiones. Cada sesión incluye la
configuración utilizada, clasificación, telemetría, sectores, eventos, clima y evolución
de pista, y puede recuperarse desde el historial entre ejecuciones.

## Controles de calidad aplicados

| Control | Estado |
|---|---|
| Compilación | Código principal y pruebas compilan con Java 17. |
| Pruebas automatizadas | **150 pruebas** ejecutadas correctamente con el wrapper de Maven. |
| Integridad FXML | `ViewsLoadTest` y `ExploreViewsLoadTest` comprueban la carga de las vistas FXML. |
| Persistencia histórica | JSON compatible con sesiones antiguas; el campo `analisis` ya retirado se ignora al leer. |
| Inmutabilidad | Snapshots, eventos, sectores y evolución de pista son inmutables o se exponen como copias. |
| Trazabilidad | Documentos por funcionalidad y matriz actualizada de épicas/HU/RF/RNF. |

## Recursos visuales y multimedia

Los renders, banderas, logos, trazados y textura se empaquetan como recursos locales.
También se incluye Titillium Web y un vídeo optimizado para el fondo del menú, con una
imagen estática animada como respaldo si JavaFX no puede reproducirlo.

La intro, el menú y el shell comparten una sola `Scene`; las transiciones liberan los
reproductores y animaciones al abandonar cada pantalla para no consumir recursos en
segundo plano.
