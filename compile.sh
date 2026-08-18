#!/usr/bin/env bash
set -euo pipefail
mkdir -p build
kotlinc hello.kt $(find src/main/kotlin -name '*.kt' | sort) -d build/frank-bones.jar
echo "Built build/frank-bones.jar"
