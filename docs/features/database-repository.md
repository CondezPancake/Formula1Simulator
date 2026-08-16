# feature/database-repository

## Qué se implementó

Capa de persistencia sobre MongoDB (RF-18, RNF-03).

## Clases

- `com.formula1.database.MongoConnection`: **[patrón Singleton]** — `getInstance()`, `getDatabase()`, `close()`. Lee `MONGO_URI`/`MONGO_DATABASE` de variables de entorno con valores por defecto (`mongodb://localhost:27017`, `formula1simulator`). Envuelve errores de conexión en `DatabaseException`.
- `com.formula1.repository.CrudRepository<T, ID>`: **[patrón Repository]** interfaz genérica (`save`, `findById`, `findAll`, `update`, `deleteById`) reutilizada por todos los repositorios concretos, evitando duplicar firmas.
- `DriverRepository/Impl`, `TeamRepository/Impl`, `VehicleRepository/Impl`, `CircuitRepository/Impl`, `ResultRepository/Impl`: cada `*Impl` obtiene su `MongoCollection<Document>` desde `MongoConnection` en el constructor.

## Patrón de diseño

- **Repository** — aísla el acceso a MongoDB del resto del sistema (RNF-26).
- **Singleton** — `MongoConnection` garantiza una única conexión compartida.

## Pendiente

Los métodos CRUD de cada `*Impl` lanzan `UnsupportedOperationException("TODO: implementar mapeo <Entidad> <-> Document en MongoDB")`: mapear cada entidad de dominio a `org.bson.Document` y viceversa es lógica de negocio real (nivel C), pendiente para cuando se implemente la persistencia efectiva. Un fallo ruidoso aquí es preferible a devolver datos falsos silenciosamente.

## Verificación

`mvn -f simulator/pom.xml clean compile` → `BUILD SUCCESS`, 38 archivos fuente compilados. No requiere una instancia de MongoDB corriendo para compilar (la conexión solo se abre al instanciar un repositorio).
