# Residual Commitment Ground Layer

> **Frank’s memory is not what happened. Frank’s memory is the residual force that what happened left behind.**

## Conceptual lock

Frank's persistent cognitive source of truth is a **Residual Commitment Field**.

A **Residual Commitment** is the entire compact tuple, not merely a scalar weight:

- **locus** — where in cognitive possibility space the commitment applies
- **polarity** — which local alternative/direction is preferred or resisted
- **residual force** — how strongly prior cognitive work still biases future cognition
- **contextual binding** — how tightly that bias is bound to a particular context
- **temporal persistence** — how the commitment survives, decays, or remains available through time
- **optional provenance** — an expensive link allocated only when later explanation/audit is worth the storage cost

The proposition is **not** the ground record. A proposition is a projection of one or more commitments.

Entities, relations, beliefs, goals, episodes, temporal chains, activation maps, and even the self-model are therefore:

1. materialized views over the commitment field,
2. secondary indices over the field, or
3. working-memory structures derived from the field.

They may be persisted as caches for speed, but they must remain **discardable and reconstructable**. They are never authoritative source-of-truth state.

## Hard reconstruction invariant

> **If only the Residual Commitment Field survives, Frank must be able to reconstruct a coherent self.**

Any persistent cache whose loss makes this impossible has accidentally become ground truth and violates the architecture.

## Provenance rule

Provenance is intentionally optional and comparatively expensive.

The default commitment record must not pay a permanent pointer cost for provenance. High-stakes commitments, explicit learning events, contested commitments, or commitments that require later audit/explanation may allocate an auxiliary provenance entry. Ordinary low-value commitments should remain completely inline.

## Why the inversion matters

Conventional packed cognition still tends to begin with something equivalent to:

```text
subject -> relation -> object -> confidence -> flags -> time
```

That representation assumes the proposition is primary and persistence/activation is metadata.

Residual-commitment architecture reverses this:

```text
persistent local cognitive bias
        ↓
projection / interpretation
        ↓
entity, relation, belief, goal, episode, self-model ...
```

Contradiction is therefore not necessarily a database conflict. Opposed live commitments may coexist at overlapping loci. The tension itself can be cognitively meaningful and context can determine which force dominates at a given moment.

Forgetting is likewise not deletion of a proposition. It can be reduction of residual force, weakening of contextual binding, altered temporal persistence, or replacement by stronger local commitments.

## Candidate A: 128-bit experimental atom

**This layout is explicitly experimental and is NOT an on-disk format commitment yet.**

The first pressure-test uses 128 bits because 64 bits appears too restrictive if the atom must remain independently reversible, contextual, temporally meaningful, and mostly self-contained.

```text
128 bits / 16 bytes

word 0
  locus              48 bits
  context_tag        16 bits

word 1
  residual_force      8 bits
  polarity             1 bit
  binding_strength     7 bits
  temporal_anchor     24 bits
  persistence_class    8 bits
  aux_ref             16 bits
```

Interpretation:

- `locus` is a compact address in cognitive space, not an EntityId or proposition hash by definition.
- `context_tag` is a local discriminator used to prevent nearby meanings from collapsing when context materially changes interpretation.
- `residual_force` is quantized 0..255.
- `polarity` is directional/oppositional state, not truth/falsity.
- `binding_strength` expresses how strongly context constrains the commitment.
- `temporal_anchor` is a compact generation/time reference; wraparound must be handled relative to the image generation epoch.
- `persistence_class` selects a decay/retention regime rather than storing a large timestamp/half-life tuple in every atom.
- `aux_ref == 0` means no expensive auxiliary payload. Non-zero values address an auxiliary segment/local pool, allowing provenance or rare richer payloads without bloating ordinary commitments.

The exact widths are subject to destruction by benchmarks. Candidate A exists to give the theory something concrete to attack.

## Pressure tests for the bit layout

A viable atom must pass all of these without secretly moving ground truth into a secondary object graph.

### 1. Reversibility

Pack -> unpack must preserve every primitive exactly.

A symbolic projection may be lossy or inferred; the ground tuple itself may not be.

### 2. Locality

Commitments likely to compete or reinforce each other should be discoverable with bounded local work. If every cognitive update requires a global scan, the locus scheme has failed.

### 3. Contradiction

Two opposed commitments must coexist without one overwriting the other. Querying a context should be capable of yielding tension rather than forcing premature resolution.

### 4. Natural decay

A commitment's effective force should be computable from compact fields and current generation/time without mutating every record on every tick.

### 5. Reinforcement

A new compatible experience should update or add commitment state cheaply without reconstructing a symbolic graph first.

### 6. Context sensitivity

The same apparent symbolic proposition must be able to behave differently under different contexts without cloning a large proposition record.

### 7. Optional provenance

The common case must remain 16 bytes. Attaching provenance must not change the base record width or require every commitment to reserve a full machine pointer.

### 8. Reconstruction

Delete all materialized Entity/Relation/Belief/Goal/Episode/Self indices and reconstruct coherent projections from the commitment field plus the minimal interpretation machinery.

### 9. Neural hand-off

Selecting high-force commitments in relevant loci should produce a compact activation set without first materializing the entire symbolic graph or replaying conversational history.

### 10. Scale

The base arena cost for Candidate A is:

```text
1,000,000 commitments  ~= 15.26 MiB
10,000,000 commitments ~= 152.59 MiB
```

Any required index overhead must be measured separately and treated as a cache cost, not quietly folded into the atom.

## Things deliberately NOT decided yet

- whether loci are hashes, learned addresses, hyperdimensional coordinates, hierarchical addresses, or a hybrid
- whether polarity remains one bit or becomes a small directional code
- the exact decay equation / persistence classes
- whether context belongs partly in the locus rather than as an explicit tag
- how auxiliary references are segmented beyond 65,535 local entries
- whether 96 bits is a superior eventual packing target
- whether multiple commitments at the same locus are stored adjacently, bucketed, or indexed separately
- how symbolic projections learn their interpretation rules
- how the neural substrate writes candidate commitments without corrupting locality

Those are research questions. Freezing them before benchmarks would recreate the exact ontology-first mistake this architecture is trying to avoid.
