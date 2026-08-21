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
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.DevelopmentalSleepAndValuesTest
java -cp build/frank-tests.jar:build/frank-bones.jar frank.tests.GoalControlTestsKt
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.NewbornStateTests
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.NewbornLearningLoopTests
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.HomeostaticTensionTests
java -cp build/frank-tests.jar:build/frank-bones.jar frank.cognition.NewbornDynamicsTests

if [[ -d prototype/python ]]; then
  (cd prototype/python && python -m pytest -q)
fi
