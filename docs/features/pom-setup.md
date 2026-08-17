# Configuración Maven

## Qué se implementó

`simulator/pom.xml` define la base de build del proyecto:

- Java 17 mediante `maven-compiler-plugin` y `<release>17</release>`.
- JavaFX 17.0.10 con `javafx-controls` y `javafx-fxml`.
- MongoDB driver sync 5.1.1.
- Jackson Databind 2.17.2 para serializar sesiones y datos.
- JUnit Jupiter 5.10.3 con Surefire 3.2.5.
- `javafx-maven-plugin` para ejecutar `com.formula1.App`.

## Estado

No queda trabajo pendiente en la configuración de build. En este entorno `mvn` no está disponible en PATH, por eso la verificación se hizo con `javac` y los jars locales de `.m2`.

## Verificación

Código principal y pruebas compilan con Java 17; la suite completa ejecuta 94 pruebas correctamente.
