# feature/javafx-bootstrap

## Qué se implementó

Arranque de la aplicación JavaFX (RNF-02) y las vistas placeholder para cada sección.

## Clases y recursos

- `com.formula1.Main`: launcher plano — `App.launch(App.class, args)`.
- `com.formula1.App extends Application`: carga `views/dashboard.fxml`, aplica `css/style.css` y muestra el `Stage` (1024x768).
- `src/main/resources/views/dashboard.fxml`: vista principal con el título y botones de navegación (aún sin lógica de cambio de pantalla).
- `views/drivers.fxml`, `teams.fxml`, `vehicles.fxml`, `circuits.fxml`: cada una con un `TableView` (`fx:id` que coincide con el campo `@FXML` del controller correspondiente) y botones "Agregar"/"Eliminar" enlazados (`onAction="#onAddXxx"`/`"#onDeleteXxx"`) a los handlers vacíos creados en `feature/controllers`.
- `views/simulation.fxml`: botón "Iniciar Clasificación" enlazado a `SimulationController#onStartQualifying` y `TableView<Result>`.
- `css/style.css`: paleta oscura estilo F1 (rojo `#e10600` sobre fondo `#15151e`).

## Decisión de diseño

`Main` y `App` se mantienen en clases separadas (`Main` no extiende `Application`) para evitar el error clásico *"JavaFX runtime components are missing"* al empaquetar un jar ejecutable, donde el launcher detecta por reflexión que la clase principal extiende `Application` y bloquea el arranque sin module-path explícito.

## Patrón de diseño

No aplica (bootstrap de UI). Es el punto donde JavaFX instancia los controllers de `feature/controllers` vía `fx:controller`.

## Pendiente

Los botones de navegación de `dashboard.fxml` son solo visuales — no hay todavía un mecanismo de cambio de escena/vista (`SceneManager` o similar); cuando se implemente, se conectarán a los `onAction` correspondientes.

## Verificación

- `mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 64 archivos fuente + 7 recursos copiados.
- `mvn -f simulator/pom.xml javafx:run` → arranca `com.formula1.App`, carga `dashboard.fxml` con el CSS aplicado y queda esperando en el hilo de JavaFX sin excepciones (verificado en este entorno: el proceso permanece vivo y sin trazas de error en el log tras el arranque).
