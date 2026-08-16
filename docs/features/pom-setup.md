# feature/pom-setup

## Qué se implementó

Configuración base de Maven para soportar el resto del proyecto (JavaFX, MongoDB, OpenF1/Jackson, JUnit 5).

## Cambios

- `simulator/pom.xml`:
  - Dependencias: `javafx-controls`, `javafx-fxml` (17.0.10), `mongodb-driver-sync` (5.1.1), `jackson-databind` + `jackson-datatype-jsr310` (2.17.2), `junit-jupiter` (5.10.3, scope `test`).
  - `maven-compiler-plugin` con `<release>17</release>` en lugar de las properties sueltas `maven.compiler.source/target`.
  - `javafx-maven-plugin` (0.0.8) configurado con `mainClass=com.formula1.App`, para poder ejecutar la app con `mvn javafx:run` una vez exista esa clase (se crea en `feature/javafx-bootstrap`).
  - `maven-surefire-plugin` (3.2.5) para que `mvn test` ejecute JUnit 5 correctamente.

## Patrón de diseño

No aplica (configuración de build).

## Pendiente

Nada de este paquete queda pendiente; es la base para todas las demás features.

## Verificación

`mvn -f simulator/pom.xml compile` → `BUILD SUCCESS`.
