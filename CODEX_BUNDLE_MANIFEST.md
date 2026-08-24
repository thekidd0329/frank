# Codex bundle manifest

If Codex cannot see the files below, STOP and report that the project bundle was not attached/extracted correctly. Do not attempt implementation in an empty workspace.

Required root files:

- `READ_ME_FIRST.txt`
- `README.md`
- `CODEX_LEARNING_MODE.md`
- `TEACHING_SEQUENCE.md`
- `LEARNING_LAB_ENVIRONMENTS.md`
- `run-learning-mode.sh`
- `teach.sh`
- `test.sh`
- `compile.sh`

Required source trees:

- `src/main/kotlin/`
- `src/test/kotlin/`
- `docs/`

Before implementation:

```bash
pwd
find . -maxdepth 2 -type f | sort | head -100
```

Then verify:

```bash
test -f CODEX_LEARNING_MODE.md
test -f TEACHING_SEQUENCE.md
test -f LEARNING_LAB_ENVIRONMENTS.md
test -d src/main/kotlin
test -d src/test/kotlin
```

If any command fails, the workspace is incomplete.

If all checks pass, read the required documents in `CODEX_LEARNING_MODE.md`, then run:

```bash
./test.sh
```

Do not add Android or product-platform work. Current scope is the Linux-only continuous visual developmental learning laboratory.
