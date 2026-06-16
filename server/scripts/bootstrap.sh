#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
h2_version="2.3.232"
h2_jar="server/lib/h2-${h2_version}.jar"

if [[ -f "$h2_jar" ]]; then
  exit 0
fi

mkdir -p server/lib
curl -L --fail \
  "https://repo1.maven.org/maven2/com/h2database/h2/${h2_version}/h2-${h2_version}.jar" \
  -o "$h2_jar"
