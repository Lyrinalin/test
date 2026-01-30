#!/usr/bin/env bash
set -euo pipefail

SERVER_PLUGINS_DIR="/path/to/your/server/plugins"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

gradle clean build

JAR_PATH="$(ls build/libs/MeteorBow-*.jar | head -n 1)"

if [[ ! -d "$SERVER_PLUGINS_DIR" ]]; then
  echo "Plugins directory not found: $SERVER_PLUGINS_DIR"
  exit 1
fi

cp "$JAR_PATH" "$SERVER_PLUGINS_DIR/"

echo "Copied $JAR_PATH to $SERVER_PLUGINS_DIR"

echo "If this script is not executable, run: chmod +x scripts/build_and_copy.sh"
