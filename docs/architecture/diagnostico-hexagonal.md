Diagnóstico actual
No se modificó, eliminó, renombró ni movió ningún archivo. El árbol de trabajo estaba limpio durante la revisión. No se inició ninguna fase de migración.
El proyecto ya utiliza MySQL mediante JDBC. En el código actual no aparecen dependencias, imports, configuraciones ni clases de MongoDB. Por tanto, las fases 3 y 8 deben tratarse como verificación y consolidación, no como una sustitución todavía pendiente.
1. Estructura y responsabilidades reales
El proyecto contiene aproximadamente:
- 127 clases Java de producción.
- 37 clases de prueba.
- 20 archivos FXML.
- 142 recursos.
- 19.492 líneas Java de producción.
Arranque y navegación
- [Main.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/Main.java): punto de entrada.
- [App.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/App.java): ciclo de vida JavaFX, carga inicial de datos, creación de FXMLLoader, escenas y Stage.
- [Navigator.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/util/Navigator.java): navegación global, carga FXML, caché de vistas y conservación del estado de la simulación.
App y Navigator forman actualmente parte de la infraestructura JavaFX, pero también realizan composición manual de dependencias.
Modelo y reglas de simulación
El paquete model contiene entidades, configuración, resultados y snapshots:
- Driver, Team, Vehicle, Circuit.
- SimulationConfig, QualifyingSession, LapResult.
- Clima, telemetría, sectores, neumáticos, paradas, eventos y evolución de pista.
- Enumeraciones que contienen factores y comportamiento de negocio.
Gran parte corresponde al dominio, aunque existen dos acoplamientos:
- Algunas clases están anotadas con Jackson, por lo que el dominio conoce detalles de serialización.
- Algunos modelos de clasificación en vivo funcionan más como DTO de aplicación o presentación que como entidades centrales.
El paquete event contiene abstracciones y reglas de eventos de simulación. En general, es lógica de dominio bien delimitada.
Servicios
Los servicios de catálogo (DriverService, TeamService, VehicleService y CircuitService) mezclan:
- Consulta y búsqueda.
- Validación.
- Operaciones CRUD.
- Acceso al singleton DataStore.
[QualifyingService.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/service/QualifyingService.java) es el principal punto de concentración de responsabilidades:
- Orquesta la simulación.
- Calcula clasificación y vueltas.
- Coordina clima, neumáticos, paradas, eventos, sectores y telemetría.
- Gestiona callbacks y repetición en vivo.
- Guarda sesiones en el historial.
- Crea directamente objetos javafx.concurrent.Task.
Esto acopla un caso de uso central con JavaFX y persistencia.
Los servicios de cálculo y las políticas, como LapTimeCalculator, PitStopPolicy, TireCompoundPolicy, PitStopService, TireStrategyService y los servicios de clima, sectores y telemetría, están más cerca de responsabilidades de dominio independientes.
Persistencia
[PersistencePort.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/data/PersistencePort.java) ya actúa como un primer puerto de salida neutral, aunque:
- Está ubicado en el mismo paquete que MySQL.
- Reúne catálogos, sesiones y todas las operaciones CRUD en una única interfaz.
- Expone una responsabilidad demasiado amplia.
[MySqlPersistenceAdapter.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/data/MySqlPersistenceAdapter.java) implementa ese contrato y delega en:
- [MySqlCatalogRepository.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/data/MySqlCatalogRepository.java)
- [MySqlSessionRepository.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/data/MySqlSessionRepository.java)
- [DatabaseConnection.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/data/DatabaseConnection.java)
La infraestructura JDBC está razonablemente concentrada en data.
[DataStore.java](/home/camper/Formula1Simulator/simulator/src/main/java/com/formula1/data/DataStore.java) es actualmente un punto crítico porque:
- Es singleton global.
- Mantiene cachés en memoria.
- Carga seeds JSON.
- Construye por defecto MySqlPersistenceAdapter.
- Coordina persistencia, fallback y errores.
- Gestiona catálogos, relaciones, configuración actual e historial.
- Es consumido directamente por servicios y controladores.
Además, las operaciones actualizan primero la memoria. Si MySQL falla, puede quedar una diferencia entre lo mostrado por la interfaz y lo realmente guardado.
2. JavaFX y FXML
Los controladores dependen con frecuencia de constructores sin argumentos porque los FXML declaran fx:controller. Esos constructores crean servicios o consultan DataStore.getInstance().
Las rutas y contratos FXML actuales incluyen:
- 20 archivos FXML.
- Controladores declarados mediante fx:controller.
- Inclusiones anidadas en config-historial.fxml, explorar.fxml, gestion.fxml y simulation.fxml.
- Campos enlazados mediante fx:id.
- Numerosos manejadores onAction.
También existen componentes de presentación en el paquete controller, entre ellos presenters de telemetría, radio, paradas y neumáticos, además de CircuitoEnVivo, MapaProgreso y Forms.
3. Acoplamientos y responsabilidades excesivas
Los principales puntos son:
1. QualifyingService: simulación, persistencia, reproducción en vivo y JavaFX.
2. DataStore: caché, persistencia, seeds, estado global y configuración.
3. SimulationController: 1.695 líneas y coordinación extensa de interfaz, simulación y resultados.
4. Navigator: estado global, caché de nodos y construcción FXML.
5. App: ciclo JavaFX, carga de datos y composición.
6. Servicios CRUD: dependen directamente del singleton.
7. Controladores: algunos crean directamente sus servicios.
8. MySqlPersistenceAdapter: crea internamente sus repositorios concretos.
9. Modelos: algunas clases de dominio conocen Jackson.
Esto dificulta sustituir componentes, probar casos de uso sin JavaFX y distinguir errores de memoria de errores de MySQL.
4. Ubicación hexagonal propuesta
Grupo actual	Ubicación conceptual propuesta
Entidades, valores, enums y reglas de simulación	domain/model
Calculadoras, políticas, eventos y reglas puras	domain/service
Orquestación de simulación, catálogos, historial y configuración	application/usecase
Contratos invocados por JavaFX	application/port/in
Contratos de repositorio y servicios externos	application/port/out
Controladores, presenters y tareas JavaFX	adapter/in/javafx
JDBC y repositorios MySQL	adapter/out/mysql
Carga de seeds	adapter/out/seed
Audio, TTS e imágenes	adapter/out/media o infraestructura de presentación
Arranque y composición	bootstrap


Esta es una ubicación objetivo. No implica mover todavía archivos. Los paquetes se cambiarían únicamente en la fase 7 y por grupos pequeños.
5. Riesgos principales
- Romper la carga FXML al cambiar constructores o nombres de controladores.
- Romper campos derivados de fx:id y controladores incluidos mediante fx:include.
- Crear instancias duplicadas si App y Navigator no comparten la misma composición.
- Perder el estado que actualmente conserva la caché estática de Navigator.
- Ejecutar actualizaciones visuales fuera del JavaFX Application Thread.
- Alterar cancelación, progreso o callbacks de Task.
- Cambiar el orden aleatorio o temporal de la simulación.
- Duplicar la carga inicial de DataStore.
- Perder detalles del historial al guardar o reconstruir sesiones y tablas hijas.
- Romper claves foráneas al renombrar pilotos, equipos, vehículos o circuitos.
- Confundir una suite verde con una integración MySQL real si las pruebas omiten la conexión cuando la base no está disponible.
- Romper los seeds al retirar prematuramente las anotaciones Jackson.
- Eliminar DataStore creyendo que es código legado, aunque actualmente sigue activo.
- Considerar terminada la eliminación de MongoDB sin verificar también pom.xml, configuración, documentación, recursos y scripts.
Plan gradual
Cada fase se ejecutará solamente después de autorización expresa. Dentro de una fase, cada paso será pequeño, independiente y terminará con compilación.
Fase 0: diagnóstico y línea base
Archivos afectados: ninguno.
Cambios: ninguno. Esta respuesta constituye el diagnóstico estático inicial.
Riesgos: que la documentación y las pruebas existentes no reflejen el comportamiento real de la aplicación o de una instancia MySQL concreta.
Comprobación: inspección de fuentes, FXML, pruebas, dependencias y estado de Git.
Terminada cuando: se apruebe este diagnóstico y se decida comenzar la línea base ejecutable.
Fase 1: proteger el comportamiento
Archivos reales implicados:
- [pom.xml](/home/camper/Formula1Simulator/simulator/pom.xml)
- Todo simulator/src/test/java
- Los 20 FXML de simulator/src/main/resources
- Scripts de database, especialmente 05_verificacion.sql
Cambios necesarios:
- Primero ejecutar compilación y suite existente sin modificar producción.
- Registrar qué pruebas necesitan JavaFX, display o MySQL.
- Comprobar carga de todos los FXML.
- Verificar que los fx:controller, fx:id, fx:include y onAction resuelven correctamente.
- Ejecutar una comprobación real de MySQL por separado.
- Agregar pruebas de caracterización solo si se encuentran rutas críticas sin cobertura.
Riesgos:
- Pruebas JavaFX dependientes del entorno gráfico.
- Pruebas MySQL marcadas como omitidas.
- Datos residuales que hagan las pruebas no repetibles.
Comprobación:
- mvn clean test.
- Arranque manual.
- Simulación completa.
- Consulta SQL de la sesión guardada y sus tablas relacionadas.
- Revisión del historial desde la interfaz.
Terminada cuando: el proyecto compila, las pruebas tienen un resultado conocido y reproducible, los FXML cargan y una simulación puede relacionarse con registros reales en MySQL.
Fase 2: crear interfaces de repositorio neutrales
Archivos reales afectados:
- data/PersistencePort.java
- data/DataStore.java
- service/DriverService.java
- service/TeamService.java
- service/VehicleService.java
- service/CircuitService.java
- service/QualifyingService.java
- Pruebas correspondientes.
Cambios necesarios:
- Mantener inicialmente PersistencePort como contrato compatible.
- Separar gradualmente sus responsabilidades por agregado o capacidad: catálogos, sesiones e historial.
- Hacer que los servicios dependan de contratos, no de DataStore.getInstance().
- Conservar adaptadores de compatibilidad para no cambiar JavaFX todavía.
- No mover paquetes en esta fase.
Riesgos:
- Crear interfaces demasiado parecidas a las tablas SQL.
- Cambiar semántica de búsquedas, duplicados o actualización.
- Romper los constructores sin argumentos usados por FXML.
Comprobación:
- Compilación después de cada contrato.
- Pruebas de servicios con implementaciones en memoria.
- Suite completa sin tocar FXML.
Terminada cuando: los casos de uso dejan de requerir un singleton concreto y los contratos no contienen tipos JDBC ni conceptos exclusivos de MySQL.
Fase 3: consolidar MySQL detrás de los puertos
MySQL ya está implementado, por lo que esta fase no comenzará desde cero.
Archivos reales afectados:
- data/MySqlPersistenceAdapter.java
- data/MySqlCatalogRepository.java
- data/MySqlSessionRepository.java
- data/DatabaseConnection.java
- data/DataStore.java
- Scripts SQL de database
- MySqlPersistenceAdapterTest.java
Cambios necesarios:
- Adaptar la implementación existente a los puertos definidos en fase 2.
- Verificar transacciones y rollback.
- Comprobar CRUD completo de catálogos.
- Comprobar ida y vuelta completa de una sesión.
- Definir claramente cuándo una escritura se considera exitosa.
- Mantener el mecanismo anterior compatible mientras se verifica el nuevo recorrido.
Riesgos:
- Memoria actualizada y transacción SQL fallida.
- Sesiones parcialmente guardadas.
- Diferencias de nulabilidad, precisión decimal, enums o fechas.
- Credenciales o puerto distintos según el entorno.
Comprobación:
- Pruebas de contrato contra MySQL.
- Ejecutar los scripts de verificación.
- Guardar una simulación y reconstruirla desde una nueva carga de la aplicación.
- Verificar tablas hijas de resultados, sectores, clima, eventos, telemetría, pista, neumáticos y paradas.
Terminada cuando: toda operación persistente atraviesa interfaces neutrales y MySQL supera las pruebas de contrato y una prueba manual completa.
Fase 4: separar casos de uso y dominio
Archivos reales afectados:
- service/QualifyingService.java
- Servicios de catálogo.
- Servicios de clima, vueltas, sectores, telemetría, neumáticos y paradas.
- Paquete event.
- Clases relevantes de model.
- Pruebas de simulación y servicios.
Cambios necesarios:
- Separar la orquestación del caso de uso de las reglas calculables.
- Mantener políticas y cálculos independientes de JavaFX.
- Extraer el acceso al historial mediante puertos.
- Retirar gradualmente de los casos de uso la creación de Task, conservando temporalmente una fachada compatible.
- No cambiar controladores ni FXML en esta fase.
Riesgos:
- Alterar aleatoriedad, orden de eventos, progresión temporal o clasificación.
- Cambiar el momento exacto de guardado de la sesión.
- Introducir servicios sin estado donde actualmente el estado por sesión es necesario.
Comprobación:
- Pruebas deterministas con semillas controladas.
- Comparar clasificación, vueltas, eventos y snapshots antes y después.
- Compilación y suite completa después de cada extracción.
Terminada cuando: las reglas y casos de uso principales pueden ejecutarse sin inicializar JavaFX ni acceder directamente a JDBC o al singleton.
Fase 5: convertir JavaFX en adaptador de entrada
Archivos reales afectados:
- controller/SimulationController.java
- Controladores de configuración, historial y catálogos.
- Presenters y componentes visuales del paquete controller.
- util/Async.java
- util/Navigator.java
- FXML solamente para verificación; no para cambiar contratos.
Cambios necesarios:
- Hacer que los controladores invoquen casos de uso.
- Trasladar la creación y gestión de Task al adaptador JavaFX.
- Mantener los métodos y campos requeridos por FXML.
- Eliminar accesos directos a DataStore desde controladores, uno por uno.
- Conservar comportamiento, navegación y caché visual.
Riesgos:
- Actualizaciones fuera del hilo JavaFX.
- Cambios en progreso, cancelación o finalización.
- Ruptura de handlers privados llamados desde FXML.
- Pérdida del estado de una simulación al navegar.
Comprobación:
- Carga de todos los FXML.
- Navegación por todas las pantallas.
- Inicio, progreso, cancelación y finalización de simulación.
- Historial y pantallas incluidas.
- Compilación después de cada controlador.
Terminada cuando: JavaFX se limita a recopilar entradas, presentar salidas y gestionar su hilo, sin contener reglas de simulación ni acceder directamente a MySQL.
Fase 6: composición central de dependencias
Archivos reales afectados:
- App.java
- Main.java
- util/Navigator.java
- data/DataStore.java
- Constructores de controladores y servicios.
- data/MySqlPersistenceAdapter.java
Cambios necesarios:
- Crear un único punto de construcción de puertos, adaptadores, casos de uso y controladores.
- Configurar todos los FXMLLoader mediante la misma fábrica de controladores.
- Asegurar que las cargas realizadas por Navigator utilicen la misma composición.
- Mantener temporalmente constructores compatibles mientras se migran las vistas.
- Evitar crear más de una caché o conexión lógica por navegación.
Riesgos:
- Conflicto entre fx:controller y una configuración incorrecta de FXMLLoader.
- Que los FXML incluidos no reciban dependencias.
- Instancias duplicadas de servicios con estado.
- Carga de datos repetida.
Comprobación:
- Cargar individualmente cada FXML y sus inclusiones.
- Verificar identidad de dependencias compartidas.
- Ejecutar una simulación completa y navegar sin perderla.
- Compilar y ejecutar toda la suite.
Terminada cuando: la creación de implementaciones concretas está concentrada y los casos de uso no crean sus propias dependencias.
Fase 7: mover paquetes gradualmente
Archivos reales afectados: modelos, eventos, servicios, puertos, adaptadores MySQL, controladores, utilidades y sus pruebas, un grupo por paso autorizado.
Cambios necesarios:
- Mover primero dominio puro.
- Después, puertos y casos de uso.
- Luego adaptadores MySQL y seeds.
- Mover JavaFX al final.
- Mantener temporalmente los controladores en su paquete si moverlos obliga a cambiar fx:controller.
- Revisar utilidades individualmente: no todas pertenecen a la misma capa.
Riesgos:
- Imports rotos.
- Acceso package-private perdido.
- Rutas FXML o reflexión afectadas.
- Cambios masivos difíciles de revisar.
Comprobación:
- Un solo grupo por paso.
- Búsqueda de referencias anteriores.
- Compilación y suite completa en cada movimiento.
- Carga FXML adicional si el grupo está relacionado con JavaFX.
Terminada cuando: las dependencias apuntan hacia el dominio y no existen ciclos entre dominio, aplicación y adaptadores.
Fase 8: eliminar MongoDB y código obsoleto
Actualmente la búsqueda no encuentra MongoDB, por lo que la parte de MongoDB sería previsiblemente un paso de auditoría sin eliminaciones.
Archivos reales afectados:
- pom.xml
- Configuración y documentación.
- Recursos y scripts.
- Clases que el análisis de referencias demuestre que quedaron obsoletas.
- DataStore solo si ya no tiene consumidores y existe reemplazo verificado.
Cambios necesarios:
- Buscar globalmente mongo, mongodb, drivers, URI y propiedades antiguas.
- Revisar el árbol efectivo de dependencias Maven.
- Identificar código realmente no utilizado.
- Solicitar autorización explícita antes de cada eliminación.
- No eliminar seeds ni fallback solamente por no ser MySQL.
Riesgos:
- Eliminar componentes todavía usados indirectamente por FXML o reflexión.
- Confundir infraestructura en memoria con código heredado de MongoDB.
- Perder la capacidad de iniciar la aplicación si MySQL falla.
Comprobación:
- Búsqueda global sin referencias MongoDB.
- Árbol Maven sin driver MongoDB.
- Compilación, pruebas y carga de todos los FXML.
- Simulación persistida, reinicio y recuperación del historial desde MySQL.
Terminada cuando: MySQL está verificado de extremo a extremo, no queda ninguna referencia real a MongoDB y cualquier eliminación adicional fue autorizada y comprobada.