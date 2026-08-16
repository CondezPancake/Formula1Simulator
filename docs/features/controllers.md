# feature/controllers

## Qué se implementó

Controllers de JavaFX (`com.formula1.controller`) que conectan las vistas (siguiente feature, `feature/javafx-bootstrap`) con la capa de servicios.

## Clases

- `DashboardController`: fija el título del panel principal.
- `DriverController`, `TeamController`, `VehicleController`, `CircuitController`: cada uno tiene un `TableView` inyectado por `@FXML`, un `initialize()` que lo puebla con `service.findAll()` (que ya devuelve una lista vacía de forma segura si la persistencia real aún no está implementada — ver `feature/service`), y handlers `@FXML` vacíos para las acciones de alta/baja (`onAddDriver`, `onDeleteDriver`, etc.), pendientes de conectar a un formulario.
- `SimulationController`: usa `SimulationFacade` a través del contrato `SimulationService` (RNF-26: depende de la interfaz, no de la implementación concreta), con un `TableView<Result>` y un botón `onStartQualifying` aún sin lógica.

## Decisión de diseño

Cada controller recibe su servicio por constructor con un valor por defecto (`new XxxServiceImpl()`), y también expone un constructor con el servicio como parámetro — facilita instanciar el controller con un mock en pruebas futuras sin necesitar un framework de inyección de dependencias (fuera del alcance de este proyecto).

## Patrón de diseño

No introduce patrones nuevos; es el punto donde JavaFX consume Facade (`SimulationController`) y Service (el resto).

## Pendiente

Los handlers de alta/baja/edición (`onAddDriver`, `onDeleteDriver`, `onStartQualifying`, etc.) están vacíos — nivel B: no lanzan excepciones, pero tampoco hacen nada todavía. Conectarlos a formularios/diálogos reales queda para cuando se implemente la interacción completa de cada vista.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 63 archivos fuente compilados. La verificación end-to-end con `mvn javafx:run` se hace en `feature/javafx-bootstrap`, una vez existan las vistas FXML que referencian a estos controllers.
