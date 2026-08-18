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
- Calibrated-confidence execution policy: external actions inside granted scope auto-execute at >= 0.90 confidence; lower-confidence actions require confirmation
- Provisional confidence features (not falsely treated as calibrated probability)
- Compact persistent person memory: `[axis_id, value_id, confidence, recency]`
- Shared self-expanding ontology with stable numeric axis/value IDs
- Combing handoff that learns associations without persisting source files or raw source content
- Deterministic "Lexie benchmark" sandbox with stale, contradictory, sarcastic, and duplicate-history evidence

## Run

```bash
python run_demo.py
```

## Test

```bash
pytest -q
```

## Architectural rules

Frank reasons as though no human will rescue the task. The authority layer is outside cognition and decides whether a proposed side effect may execute.

Working cognition may temporarily retain rich Evidence objects, including source context needed to reason and resolve contradictions. Long-term person memory does **not** retain that source material. The combing layer collapses durable learning into compact numeric claims and discards the raw source handoff.

Ontology IDs are stable once assigned. Frank may create new axes and values when reality introduces a concept his current vocabulary does not contain, but existing IDs are never repurposed.

Current benchmark intentionally avoids live APIs. The next engineering steps are entity resolution, a capability/tool registry, and a model-driven uncertainty selector while preserving deterministic evaluation.
