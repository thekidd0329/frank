#!/usr/bin/env bash
set -euo pipefail

./compile.sh

kotlinc \
  -classpath build/frank-bones.jar \
  $(find src/test/kotlin -name '*.kt' | sort) \
  -include-runtime \
  -d build/frank-tests.jar

java -cp build/frank-tests.jar:build/frank-bones.jar frank.tests.ArchitectureTestsKt
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.ResidualCommitmentTests
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.CognitiveEnvironmentSimulation

if [[ -d prototype/python ]]; then
  (cd prototype/python && python -m pytest -q)
fi
