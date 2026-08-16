# feature/api-openf1

## Qué se implementó

Aislamiento de la integración con la API externa OpenF1 (RF-17, HU-24) del resto del sistema.

## Clases

- `com.formula1.api.F1DataProvider`: interfaz objetivo (target) del patrón Adapter — `fetchDrivers()`, `fetchTeams()`, `fetchCircuits()`.
- `com.formula1.api.OpenF1ApiAdapter implements F1DataProvider`: adapta la API REST de OpenF1 (`https://api.openf1.org/v1`) al contrato anterior. Usa `java.net.http.HttpClient` (nativo de Java 17, sin dependencia adicional) y `ObjectMapper` de Jackson para el parseo JSON.

## Patrón de diseño

**Adapter** — el resto del sistema (`service.OpenF1Service`) dependerá únicamente de `F1DataProvider`, sin conocer el formato JSON ni la URL de OpenF1. Esto permite sustituir la fuente de datos (otra API, un mock, un fixture local) sin tocar el resto del código.

## Pendiente

Los tres métodos de `OpenF1ApiAdapter` lanzan `UnsupportedOperationException` — la llamada HTTP real y el mapeo del JSON de OpenF1 a las entidades de dominio (`Driver`, `Team`, `Circuit`) es lógica de negocio real (nivel C), pendiente de implementación. `buildRequest(String path)` ya deja preparada la construcción de la petición GET para cuando se implemente.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 40 archivos fuente compilados.
