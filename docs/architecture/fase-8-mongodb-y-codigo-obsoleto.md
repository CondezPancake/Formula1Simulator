# Fase 8 — eliminar MongoDB y código obsoleto

Tal como anticipaba el diagnóstico original ("Actualmente la búsqueda no
encuentra MongoDB, por lo que la parte de MongoDB sería previsiblemente un
paso de auditoría sin eliminaciones"), esta fase es puramente de
verificación. No se eliminó ni se modificó ningún archivo de producción.

## Búsqueda de MongoDB

Búsqueda global, insensible a mayúsculas, sobre todo el repositorio (código
Java, XML, Markdown, SQL, Python, shell, JSON, properties, YAML):

```bash
grep -rli "mongo" --include="*.java" --include="*.xml" --include="*.md" \
  --include="*.sql" --include="*.py" --include="*.sh" --include="*.json" \
  --include="*.properties" --include="*.yml" --include="*.yaml" .
```

**Cero coincidencias reales.** La única mención de "mongo" en todo el
repositorio vive en `docs/architecture/diagnostico-hexagonal.md` —el propio
registro del diagnóstico original que ya explicaba por qué no había nada
que sustituir— y en los diccionarios binarios de `tools/piper/bin/espeak-ng-data/`
(`sw_dict`, etc.), coincidencias de subcadena en datos fonéticos del
sintetizador de voz Piper, sin relación alguna con bases de datos.

`find . -iname "*mongo*"` tampoco encuentra ningún archivo con ese nombre.

## Árbol de dependencias Maven

```bash
mvn dependency:tree
```

```
com.formula1:simulator:jar:1.0-SNAPSHOT
+- org.openjfx:javafx-controls:jar:17.0.10:compile
+- org.openjfx:javafx-fxml:jar:17.0.10:compile
+- org.openjfx:javafx-media:jar:17.0.10:compile
+- com.mysql:mysql-connector-j:jar:9.5.0:compile
+- com.fasterxml.jackson.core:jackson-databind:jar:2.17.2:compile
(+ junit-jupiter, alcance test)
```

Ningún driver de MongoDB, ni directo ni transitivo.

## Código obsoleto

Se escaneó cada una de las 138 clases de producción buscando si algún otro
archivo `.java` o `.fxml` del repositorio la nombra:

```bash
for f in $(find simulator/src/main/java -name "*.java"); do
  cls=$(basename "$f" .java)
  count=$(grep -rl "\b$cls\b" simulator/src --include=*.java \
    simulator/src/main/resources/views/*.fxml | grep -v "/$cls\.java$" | wc -l)
  [ "$count" -eq 0 ] && echo "POSIBLEMENTE SIN USAR: $f"
done
```

**Un solo resultado**: `com.formula1.bootstrap.Main`. No es código muerto —
es el punto de entrada de la aplicación (`public static void main`), y por
eso nada dentro del código Java lo referencia: lo invoca la JVM desde fuera,
nombrado como texto en `pom.xml` (`<mainClass>`) y en `.vscode/launch.json`
(exactamente el mismo patrón que ya causó la sorpresa del lote 6 de la
Fase 7). Confirmado como falso positivo, no como candidato a eliminar.

Ningún otro archivo de producción quedó sin una sola referencia. Después de
7 fases moviendo, dividiendo y ensanchando visibilidad, el escaneo no
encontró una sola clase huérfana — el código ya estaba razonablemente
podado antes de empezar esta migración (127 clases de producción según el
diagnóstico inicial, la misma cifra que hoy: mover no generó restos).

No se encontró nada que pedir autorización para eliminar. `DataStore` sigue
teniendo consumidores reales (`AppComposition`, los 5 servicios de
`application.usecase`, y el respaldo de compatibilidad en los constructores
sin argumentos), así que tampoco es candidato: sigue siendo el único
adaptador que implementa los cuatro puertos de aplicación.

## Verificación de cierre

- `mvn clean test`: 174/0/0, 4 *skipped* (MySQL) — misma cuenta desde la
  Fase 3.
- Carga de los 20 FXML: verificada en el lote 5 de la Fase 7
  (`ViewsLoadTest`, `ExploreViewsLoadTest`).
- **Pendiente, arrastrado desde la Fase 1**: la verificación de extremo a
  extremo contra una instancia MySQL real (guardar una sesión, reiniciar,
  recuperar el historial) sigue sin poder hacerse en esta máquina —sin
  Docker ni MySQL disponibles—, así que los 4 tests de
  `MySqlPersistenceAdapterTest` siguen en *skipped*, no en verde. Es el
  único punto de la Fase 8 que no se puede cerrar aquí; ver
  `fase-1-linea-base.md` y `fase-3-consolidar-mysql.md` para los pasos
  exactos a correr en una máquina con MySQL levantado.

## Veredicto

No queda ninguna referencia real a MongoDB, el árbol de dependencias está
limpio, y el escaneo de código obsoleto no encontró nada que eliminar —por
lo tanto nada que pedir autorización para eliminar. La única pieza que
queda abierta de las 8 fases es la verificación MySQL en vivo, que depende
de un entorno que esta máquina no tiene.
