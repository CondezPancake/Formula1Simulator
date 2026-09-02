#!/usr/bin/env bash
# Arranca el simulador.
#
# Funciona desde cualquier directorio: se sitúa en la raíz del repositorio
# antes de invocar a Maven, así que no depende de dónde se llame ni de
# recordar el -f simulator/pom.xml.
#
# Usa el wrapper (./mvnw) en vez de un Maven del sistema: así basta con tener
# un JDK 17 para clonar y ejecutar, sin instalar Maven aparte. La primera
# ejecución se descarga la distribución una sola vez.
set -euo pipefail

cd "$(dirname "$0")"

# El wrapper exige JAVA_HOME. Si no está puesto, se deduce del java del PATH,
# que es lo habitual en una máquina con el JDK instalado pero sin variables.
if [[ -z "${JAVA_HOME:-}" ]]; then
    if command -v java >/dev/null 2>&1; then
        java_bin="$(command -v java)"
        # readlink -f resuelve el enlace cuando java es un symlink a la JDK.
        java_bin="$(readlink -f "$java_bin" 2>/dev/null || echo "$java_bin")"
        JAVA_HOME="$(dirname "$(dirname "$java_bin")")"
        export JAVA_HOME
    else
        echo "No encuentro Java. Instala un JDK 17 o define JAVA_HOME." >&2
        exit 1
    fi
fi

exec ./mvnw -f simulator/pom.xml javafx:run "$@"
