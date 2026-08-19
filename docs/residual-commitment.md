# Residual Commitment Ground Layer

> **Frank’s memory is not what happened. Frank’s memory is the residual force that what happened left behind.**

## Conceptual lock

Frank's persistent cognitive source of truth is a **Residual Commitment Field**.

A durable **Residual Commitment** now has exactly three semantic variables:

```text
locus_id          // WHERE the force lives
net_force         // DIRECTION + STRENGTH, signed
last_updated_tick // WHEN it was last materially maintained
```

Everything else must prove that it deserves ground-state storage.

The proposition is **not** the ground record. Entities, relations, beliefs, goals, episodes, temporal chains, activation maps, and self-model views are projections, secondary indices, or working structures over the commitment field. They may be cached, but those caches remain discardable and reconstructable.

## Hard reconstruction invariant

> **If only the Residual Commitment Field survives, Frank must be able to reconstruct a coherent cognitive state.**

Any supposedly secondary structure whose loss makes continuation impossible has accidentally become ground truth and violates the architecture.

## Locus

**Locus is the address of a commitment inside Frank's cognitive space.**

It answers:

> Where does this residual force live?

A locus has two jobs:

1. **Identity** — the same learned meaning should retain the same address across time.
2. **Topology** — related meanings should be local enough that activation, scanning, association, and decay policy can exploit neighborhood structure.

Most of the system should treat a locus as opaque. Only locus construction, addressing, and the commitment field should care how its bits acquire structure.

The address assignment procedure must not smuggle in a propositional ontology. If a new meaning can only receive a good locus by looking up a human- or model-defined concept first, the inversion has failed.

The exact novel-locus construction algorithm remains an open research problem.

## Signed force

`net_force` carries both polarity and magnitude.

```text
negative  -> push / opposing direction
zero      -> equilibrium / no durable directional commitment
positive  -> pull / supporting direction
```

A separate polarity field is redundant.

Reinforcement and contradiction are signed pressure on the same variable. A simple experimental update is:

```text
F_decayed = F_prior * retention(locus) ^ delta_ticks
F_new     = clamp(F_decayed + evidence_force, -1.0, +1.0)
```

This is an experimental mechanics rule, not yet a frozen cognitive law.

## Sparse tree rule

The Residual Commitment Field does **not** preallocate empty meanings.

Absence of a locus means no lasting commitment currently exists there.

Zero is therefore a transition/equilibrium state, not a durable row. If cancellation or decay drives a commitment to the configured prune threshold, the record is removed.

```text
raw pattern
    ↓
temporary pressure
    ↓
enough directional force?
   / \
 no   yes
 ↓     ↓
die   create locus
         ↓
      maintain / oppose
         ↓
      signed net force
         ↓
      unsupported
         ↓
      lazy decay
         ↓
      near neutral
         ↓
        prune
```

The tree contains only branches that have actually borne enough fruit to persist.

## Structured decay without a bloated atom

Decay does not need per-record `contextual_binding`, `flags`, `persistence_class`, or a stored half-life.

The current simplification is:

```text
decay behavior = function(locus topology, abs(net_force), age)
```

The locus can eventually encode or imply the neighborhood/region that governs decay. Force and age provide the other two inputs.

This keeps the atom small while preserving the ability for different cognitive regions to age differently.

The exact locus-topology-to-decay mapping is deliberately **not** frozen yet because defining those regions top-down could accidentally recreate an ontology.

## Candidate A: experimental 128-bit pressure test

**Candidate A is experimental. It is NOT a frozen `frank.cog` ABI.**

The current three-variable pressure-test packing is:

```text
128 bits / 16 bytes

word 0
  locus_id            64 bits

word 1
  net_force           32 bits  // IEEE-754 f32 bits
  last_updated_tick   32 bits  // relative/local logical tick
```

This packing has a useful property: every bit currently belongs to one of the three actual semantic variables. There are no speculative `kind`, `flags`, context, or inline-provenance fields consuming space just because bits are available.

The 32-bit tick is a pressure-test choice only. Epoch, wrap, and long-horizon requirements must be measured. If those measurements require more temporal width, Candidate A should be changed rather than defended.

Base arena density remains:

```text
1,000,000 commitments  ~= 15.26 MiB
10,000,000 commitments ~= 152.59 MiB
```

before indexes/caches.

## What was removed from the ground atom

The earlier fuller tuple included:

- separate polarity
- contextual binding
- persistence class / temporal tuple
- universal `kind`
- flags
- inline optional provenance handle

Those are no longer assumed to belong in every commitment.

Current rule:

> **Everything has to earn its bit.**

If later experiments prove some distinction is required for reconstruction and cannot be derived from locus, force, time, or a reconstructable auxiliary structure, then it can return to the ground schema with evidence.

## `frank.cog` arena authority

The **Residual Commitment arena is authoritative ground state**.

Entity, Relation, Episode, Goal, Belief, Activation, TemporalIndex and similar arenas may still exist as persistent caches or compatibility views for speed. Their arena IDs are retained so the schema can evolve without gratuitously invalidating older branch images, but they are not independent cognitive truth.

Provenance and string pools may preserve auxiliary evidence/explanation material when justified, but must not silently become required hidden cognitive state unless the reconstruction boundary is explicitly revised.

## Current open research question

> **How do we construct a stable, locality-preserving locus for a meaning Frank has never encountered before, without an ontology secretly choosing the meaning for him?**

Viable directions under investigation include:

- content-derived locality-preserving projection/hash;
- online topological allocation near active/similar commitments;
- hyperdimensional/vector-symbolic addressing;
- a tiny non-propositional bootstrap orientation.

None is settled.

## Credits

- **Christian / Thekidd0329** — project owner and architecture controller; defined locus as the address where residual force is parked, drove the three-variable atom, the sparse fruit-bearing-tree rule, and the decision to let topology govern decay rather than bloating every record.
- **Grok** — introduced/pushed the Residual Commitment inversion that moved persistence away from proposition-first memory.
- **Gemini** — adversarially pressure-tested the atom, locus construction, decay, reconstruction, and ontology-contamination risks that led directly to this simplification pass.
- **ChatGPT** — systems integration: reconciled the relay outputs with repository contracts and translated the current decision into code/docs/schema changes.
- **Claude / Copilot and the wider gauntlet** — structural simulation and implementation-hole stages remain downstream checks before this experiment should be treated as settled.
