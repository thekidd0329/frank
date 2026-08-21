#!/usr/bin/env bash
set -euo pipefail

./compile.sh
exec kotlin -classpath build/frank-bones.jar frank.teacher.TeachMainKt "$@"
