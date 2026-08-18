# Frank Core — Brick One

This prototype implements the first vertical slice of Frank's cognitive architecture:

- Goal + CognitiveState representation
- Evidence with source reliability/freshness
- Local JSON/text parsing into structured evidence axes, values, polarity, identity confidence, and timestamps
- Per-evidence freshness decay with a literal 14-day half-life
- Explicit support/contradiction links
- Deterministic semantic-opposite detection for parsed evidence
- Autonomous read-only planner loop
- Information-gain-driven action candidates
- Stopping criteria
- Authority firewall kept outside cognition
- Provisional confidence features (not falsely treated as calibrated probability)
- Deterministic "Lexie benchmark" sandbox with stale, contradictory, sarcastic, and duplicate-history evidence

## Run

```bash
python run_demo.py
```

## Test

```bash
pytest -q
```

## Architectural rule

Frank reasons as though no human will rescue the task. The authority layer only decides whether a proposed side effect may execute.

Current benchmark intentionally avoids live APIs. The next engineering step is replacing the hardcoded benchmark query sequence with a model-driven uncertainty selector while preserving deterministic evaluation.
