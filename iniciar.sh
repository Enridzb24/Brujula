#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
printf '%s\n' 'Brújula · Abre http://localhost:8091 cuando termine el inicio.'
java -Duser.timezone=America/Lima -jar brujula.jar
