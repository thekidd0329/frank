#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if ! command -v kotlinc >/dev/null 2>&1; then
  echo "Frank needs Kotlin (kotlinc) installed." >&2
  echo "Ubuntu/Debian: install a JDK and Kotlin compiler, or use the repo CI toolchain." >&2
  exit 1
fi

./compile.sh
exec kotlin -classpath build/frank-bones.jar frank.cli.TeachMain "$@"
