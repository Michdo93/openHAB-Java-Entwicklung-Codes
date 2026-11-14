#!/bin/sh
# Startet ein Beispiel direkt aus dem Quelltext – mit den Zugangsdaten aus .env
# und den Bibliotheken aus lib/ (einmal: mvn dependency:copy-dependencies).
#   ./starte.sh kapitel10_erste_schritte/B01_Verbinden.java
set -a
[ -f .env ] && . ./.env
set +a
exec java --enable-native-access=ALL-UNNAMED -cp "lib/*" "$@"
