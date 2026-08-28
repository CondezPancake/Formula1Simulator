#!/usr/bin/env bash
# Arranca el simulador.
#
# Funciona desde cualquier directorio: se sitúa en la raíz del repositorio
# antes de invocar a Maven, así que no depende de dónde se llame ni de
# recordar el -f simulator/pom.xml.
set -euo pipefail

cd "$(dirname "$0")"
exec mvn -f simulator/pom.xml javafx:run "$@"
