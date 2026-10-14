#!/usr/bin/env bash
# Compiles and starts the app on macOS / Linux.
# Usage:  bash run.sh        (or:  chmod +x run.sh && ./run.sh)
set -e
cd "$(dirname "$0")"

if ! ls lib/*.jar >/dev/null 2>&1; then
    echo "No jar found in lib/. Download the H2 jar from https://h2database.com/html/download.html"
    echo "and put it in the lib folder, then run this script again."
    exit 1
fi

javac -cp "lib/*" app/*.java
java -cp ".:lib/*" app.MainApp
