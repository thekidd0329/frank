#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFEST="$ROOT/platform/android-compat/Cargo.toml"

if ! command -v cargo >/dev/null 2>&1; then
  echo "Frank needs Rust/Cargo installed." >&2
  echo "Ubuntu/Debian: sudo apt install cargo" >&2
  echo "Or install Rust with rustup: https://rustup.rs" >&2
  exit 1
fi

exec cargo run --quiet --manifest-path "$MANIFEST" --bin frank
