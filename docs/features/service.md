# feature/service

## Qué se implementó

Capa de servicios (`com.formula1.service`) entre los controllers de JavaFX y los repositorios/adapter.

## Clases

- `DriverService/Impl`, `TeamService/Impl`, `VehicleService/Impl`, `CircuitService/Impl`: CRUD sobre su repositorio correspondiente + validación de negocio en `register(...)` usando `ValidationUtils` y lanzando la excepción de dominio adecuada (`InvalidDriverException`, `InvalidVehicleConfigurationException`, `InvalidSimulationException`) cuando los datos no cumplen RNF-07 (velocidad > 0, longitud > 0, vueltas > 0, nombre ≠ vacío).
- `WeatherService/Impl`: genera condiciones climáticas iniciales de una sesión (RF-11) con `RandomUtils`. Es la única lógica "real" (no placeholder) de esta feature porque es simple y no depende de MongoDB/OpenF1; la evolución dinámica del clima (HU-29) queda fuera del alcance MVP.
- `OpenF1Service/Impl`: envuelve `api.F1DataProvider` y traduce cualquier fallo en `OpenF1ConnectionException`, para cumplir RF-21 ("informar al usuario cuando ocurra un error durante la consulta de OpenF1").
- `SimulationService`: interfaz de contrato para orquestar una sesión de clasificación (`startQualifying`, `getResults`); la implementará `simulation.SimulationFacade` en la siguiente feature.

## Decisión de diseño — nivel B vs nivel C

Los métodos `findAll()` de `Driver/Team/Vehicle/CircuitServiceImpl` capturan `RuntimeException` del repositorio (que hoy lanza `UnsupportedOperationException`, ver `feature/database-repository`) y devuelven `List.of()` con un log a `System.err`. Esto es intencional: estos métodos se invocan desde `initialize()` de los controllers para poblar tablas al arrancar la app, y no deben tumbar la UI. Las operaciones de escritura (`register/update/delete`) no capturan nada — se ejecutan solo tras una acción explícita del usuario, así que un fallo ruidoso ahí es preferible.

## Patrón de diseño

No introduce patrones nuevos; consume Repository (feature anterior) y Adapter, y expone el contrato que usará el patrón Facade.

## Pendiente

Nada propio de esta capa; el trabajo pendiente real vive en los repositorios/adapter subyacentes.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 53 archivos fuente compilados.
