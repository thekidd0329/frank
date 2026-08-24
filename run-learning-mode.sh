#!/usr/bin/env bash
set -euo pipefail

PROFILE="${FRANK_MEMORY_PROFILE:-phi}"
STATE="${FRANK_TEACHING_STATE:-/tmp/frank-learning-mode.log}"

case "$PROFILE" in
  phi|control) ;;
  *)
    echo "FRANK_MEMORY_PROFILE must be 'phi' or 'control'" >&2
    exit 2
    ;;
esac

echo "[1/2] Running Frank test suite..."
./test.sh

echo
echo "[2/2] Starting Frank learning terminal"
echo "profile=$PROFILE"
echo "journal=$STATE"
echo "Codex instructions: CODEX_LEARNING_MODE.md"
echo

exec ./teach.sh --profile "$PROFILE" --state "$STATE"
