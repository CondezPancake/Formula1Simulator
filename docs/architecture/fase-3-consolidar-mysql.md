# Fase 3 — consolidar MySQL detrás de los puertos

MySQL ya estaba implementado antes de esta migración; esta fase es
verificación y consolidación, no una sustitución desde cero (ver
[diagnóstico inicial](diagnostico-hexagonal.md)).

## Cambios de código

- **`MySqlPersistenceAdapter`** deja de contener lógica JDBC propia y pasa a
  ser una fachada delgada que compone dos adaptadores nuevos, uno por puerto
  definido en la Fase 2:
  - `MySqlCatalogPersistenceAdapter implements CatalogPersistencePort`
  - `MySqlSessionPersistenceAdapter implements SessionPersistencePort`
- El manejo de transacción (`execute`/`transaction`, con commit/rollback y
  restauración del autocommit previo) estaba duplicado dentro del adaptador
  original; se extrajo a `JdbcTransactionSupport`, compartido por los dos
  adaptadores nuevos.
- `PersistencePort` sigue siendo la única fachada que `DataStore` conoce —
  cero cambio de comportamiento, cero cambio de paquete.

Esto dejaba antes un solo archivo con las dos responsabilidades JDBC
mezcladas; ahora cada adaptador se puede sustituir, extender o probar por
separado sin arrastrar el otro (p. ej. un futuro adaptador de sesiones a otro
motor no obliga a tocar el de catálogo).

## Transacciones y rollback — verificado por lectura de código

- **Catálogos**: cada `save*`/`delete*` de `MySqlCatalogPersistenceAdapter`
  pasa por `JdbcTransactionSupport.transaction(...)`: autocommit en falso,
  commit al terminar, rollback y relanzamiento ante cualquier
  `SQLException`/`RuntimeException`, autocommit restaurado siempre en
  `finally`. Una escritura de catálogo es atómica por entidad.
- **Sesiones**: `MySqlSessionRepository.save(...)` ya gestionaba su propia
  transacción de extremo a extremo (borra lo existente, inserta
  configuración, resultados, sectores, eventos, clima, telemetría, evolución
  de pista, pit stops y cambios de neumáticos, y solo entonces hace commit;
  cualquier fallo intermedio revierte las nueve tablas). El adaptador nuevo
  no reabre otra transacción encima: delega directo. Confirmado por lectura,
  no reintroduce el riesgo de "sesión parcialmente guardada" que señalaba el
  diagnóstico.

## Cuándo se considera exitosa una escritura (pedido explícito de esta fase)

El diseño existente separa dos nociones de "éxito" y **no cambia en esta
fase** — documentarlo es el entregable, no alterarlo:

1. **Éxito para la interfaz**: ocurre en cuanto `DataStore` actualiza su mapa
   en memoria, *antes* de intentar MySQL. La UI nunca espera ni bloquea por
   la escritura durable.
2. **Éxito durable**: ocurre solo si `persistir(...)` corre sin lanzar — es
   decir, si la transacción JDBC hizo commit. Si MySQL falla,
   `DataStore.persistir` atrapa la excepción, dejar el mapa en memoria como
   ya estaba (fuente de verdad en ejecución) y solo cambia `estado` a
   *"Error al escribir en MySQL — los cambios siguen en memoria"* — no revierte
   el mapa ni relanza.

Consecuencia explícita: **la UI puede mostrar un cambio que no llegó a
MySQL** si la base falla justo después de una escritura exitosa en memoria.
Es una decisión de diseño ya tomada (memoria como fuente de verdad en
ejecución, MySQL como persistencia duradera detrás, ver `README.md` →
Arquitectura → Datos), no un defecto de esta fase. Cambiar esa semántica
—por ejemplo, exigir commit MySQL antes de considerar guardado un cambio—
sería una decisión de producto para una fase futura, con su propio análisis
de UX (¿bloquear la interfaz mientras escribe?) y no se toca aquí.

## Verificación pendiente de entorno

Esta máquina no tiene MySQL, Docker ni un cliente `mysql` disponibles (
verificado con `Test-NetConnection` al puerto 3307 y búsqueda de binarios).
`MySqlPersistenceAdapterTest` ya cubre exactamente lo que pide esta fase —

- `cargaElModeloRelacionalCompletoCuandoMySqlEstaDisponible` — CRUD completo
  de los 4 catálogos.
- `guardaYRecuperaUnaSesionEnUnaTransaccion` y
  `conservaUnaSimulacionCompletaConSusRelaciones` — ida y vuelta completa de
  una sesión con sus tablas hijas.
- `actualizaLosCuatroCatalogosSinPerderRelaciones` — CRUD sin perder
  relaciones.

— pero sus 4 tests siguen `@Test` + `assumeTrue(DatabaseConnection.isAvailable())`,
así que aquí quedan *skipped*, no verdes. Antes de dar la Fase 3 por cerrada
de verdad, hay que ejecutar en una máquina con MySQL levantado y
`database/SQL/schema.sql` + `database/SQL/data.sql` aplicados:

```bash
mvn clean test
```

y confirmar que esos 4 tests pasan (no solo que no fallen por *skip*).

## Veredicto

Adaptado a los puertos de la Fase 2, transacciones verificadas por lectura de
código, semántica de "escritura exitosa" documentada explícitamente,
`mvn clean test`: 172/0/0, 4 *skipped* por falta de MySQL local — misma
cuenta que en la línea base de Fase 1, cero regresiones. Falta solo correr
esos 4 tests contra una instancia MySQL real para cerrar la fase con
evidencia en verde, no solo por lectura.
