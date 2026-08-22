# Frank

Frank is a developmental cognition project centered on persistent residual commitments, newborn learning, and reconstructable state.

## Current focus: Linux newborn learning

The active development path is intentionally Linux-terminal first. Android integration is deferred until Frank can demonstrate persistent learning and later symbol grounding independently.

### Run the core test suite

```bash
./test.sh
```

### Run the teaching terminal

Compile first:

```bash
./compile.sh
```

Then launch the newborn teaching terminal with the ordinary control memory profile:

```bash
kotlin -classpath build/frank-bones.jar frank.teacher.TeachMainKt --profile control
```

Or launch the golden-ratio-derived memory experiment:

```bash
kotlin -classpath build/frank-bones.jar frank.teacher.TeachMainKt --profile phi
```

Inside the terminal, repeated opaque pairings can now be learned and recalled:

```text
/pair round-red-pattern :: ba
/pair round-red-pattern :: ba
/pair round-red-pattern :: ba
/pair round-red-pattern :: ba
/recall round-red-pattern
/age 1
/recall round-red-pattern
/associations
```

The pairing mechanism does not predeclare that either side has semantic meaning. It only learns repeated co-occurrence between opaque loci.

### Compare memory profiles directly

```bash
kotlin -classpath build/frank-bones.jar frank.cognition.PhiMemoryProbeKt
```

This prints the association strength and retrieval lifetime produced by the control and phi-derived memory profiles under the same deterministic experience sequence.

See `docs/NEWBORN_PRIMITIVE.md` and `docs/PHI_MEMORY_LEARNING.md` for the developmental contract.
