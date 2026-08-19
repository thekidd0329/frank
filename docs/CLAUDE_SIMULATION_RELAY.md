========================================
FRANK RELAY: STAGE 3 — STRICT CLAUDE SIMULATION
========================================

CONTEXT & OBJECTIVE

You are the execution and simulation stage in the Frank multi-model engineering relay.

Frank is a developmental cognitive system intended to begin with zero learned semantics, no vocabulary, no propositional ontology, and no external model acting as cognitive authority.

Core inversion:

> Frank's memory is not what happened. Frank's memory is the residual force that what happened left behind.

Persistent ground truth is the Residual Commitment Field. Beliefs, relations, goals, episodes, activation sets, and self-model views are rebuildable projections/caches.

CURRENT THREE-VARIABLE GROUND ATOM

```text
locus_id          // WHERE + neighborhood/topology
net_force         // DIRECTION + STRENGTH, signed [-1.0,+1.0]
last_updated_tick // WHEN last materially maintained
```

Important rules:

- Polarity is the sign of `net_force`; it is not a separate field.
- Exact/near zero is not durable state. A commitment that reaches the prune threshold is removed.
- The field is sparse. No empty tree of zero-valued loci is preallocated.
- Decay is lazy/local, not a global per-tick rewrite.
- Long-term intent is for locus topology to influence decay class, but the novel-locus topology algorithm is NOT yet settled.
- For THIS simulation only, use the fixed retention factor below so the locus-construction problem remains isolated.

RULES & CONSTRAINTS

1. Do not inject LLM reasoning, human labels, personality, or semantic interpretations into Frank's internal state.
2. Frank starts with an empty Residual Commitment Field.
3. Use fixed per-tick retention `R = 0.90` for this simulation only.
4. Lazy decay when a locus is queried or updated:

   `F_decayed = F_prior * (0.90)^(current_tick - last_updated_tick)`

5. Incoming evidence is signed force:

   `F_new = clamp(F_decayed + E_force, -1.0, +1.0)`

6. Same-sign evidence therefore reinforces naturally; opposite-sign evidence contradicts naturally.
7. Use prune threshold `epsilon = 0.000001`. If `abs(F_new) <= epsilon`, remove the locus rather than storing zero.
8. When a query materializes lazy decay, store the decayed force and set `last_updated_tick = current_tick`, unless the locus is pruned.
9. A newly observed raw hash with non-zero force creates a locus directly for this experiment. This is a temporary event grammar, NOT a solution to the open novel-locus construction problem.
10. If the simulation reaches a mechanism that is not specified—especially relational/multi-locus binding—HALT that operation and report the exact missing rule. Do not invent semantics.

STATE VARIABLES TO TRACK PER LOCUS

- `locus_id` (u64 / hexadecimal display)
- `net_force` (f32 conceptual value, -1.00 to +1.00)
- `last_updated_tick` (logical tick)

Do NOT add hidden variables such as:

- polarity enum
- contestation mass
- contextual binding
- semantic label
- entity ID
- goal flag
- provenance pointer

unless the simulation proves that the three-variable state cannot reproduce required behavior. If you believe another variable is necessary, report that as a failure/gap rather than silently adding it.

EVENT STREAM TO EXECUTE

- Tick 1: Observe raw input hash `0xA1`, evidence force `+0.40`. Create locus.
- Tick 2: Observe raw input hash `0xA1`, evidence force `+0.30`. Lazy-decay then reinforce.
- Tick 5: Query `0xA1` after three ticks of silence. Materialize lazy decay.
- Tick 6: Observe raw input hash `0xA1`, evidence force `-0.90`. Lazy-decay then contradict.
- Tick 7: Observe raw input hash `0xB2`, evidence force `+0.80`. Create second locus.
- Tick 10: Observe simultaneous `0xA1` evidence `+0.50` and `0xB2` evidence `+0.50`. Update each independently, then ATTEMPT multi-locus co-occurrence binding.
- Tick 11: Attempt projection reconstruction using ONLY the surviving Residual Commitment Field plus the stated deterministic rules.

REQUIRED DELIVERABLES

1. TICK-BY-TICK STATE TABLE
   Track every state transition for all live loci. Show the decay arithmetic and update arithmetic at each relevant tick.

2. SPARSE-FIELD CHECK
   Confirm that no durable zero row is created. If cancellation/pruning occurs, show the removal explicitly.

3. PROJECTION RECONSTRUCTION TEST AT TICK 11
   Attempt to derive the minimal defensible belief-like projection from the field without inventing semantic names.
   A valid projection may say only things mechanically supported by the state, e.g. "locus 0xA1 currently carries positive/negative residual force of magnitude X." Do not call the locus a phone, person, goal, relationship, etc.

4. MULTI-LOCUS BINDING GAP REPORT
   At Tick 10, determine whether simultaneous observation can create a relation/co-occurrence commitment under the committed rules.
   If no deterministic higher-order locus construction rule exists, HALT that binding operation and identify the exact underspecification.

5. INFORMATION-LOSS CHECK
   Determine whether two materially different evidence histories can produce the same final `(locus_id, net_force, last_updated_tick)` state and, if so, whether those histories SHOULD produce different future behavior under the currently stated architecture.
   If yes, the three-variable atom may be insufficient. If no, explain why history equivalence is acceptable.

6. LOCUS PRESSURE TEST
   Do not solve the open problem by naming concepts. State what a future novel-locus algorithm must guarantee:
   - stable identity across time;
   - locality for related learned meanings;
   - no hidden propositional ontology;
   - support for split/merge or explain why identity cannot survive those operations;
   - bounded/local lookup rather than global scanning.

7. NEXT AI RELAY PACKET
   Produce a complete relay containing:
   - settled contracts;
   - exact final residual state;
   - measured simulation results;
   - derived findings;
   - unknowns/gaps;
   - any failed invariant;
   - the single highest-leverage next engineering question.

EPISTEMIC FORMAT

Separate conclusions into:

MEASURED / EXECUTED
- direct outputs of the simulation arithmetic and repository-visible rules.

DERIVED
- logical consequences of those outputs.

UNKNOWN
- things the rules/repository do not determine.

Do not pretend an unknown is a measured fact.

STOPPING CONDITION

Finish after Tick 11, the reconstruction attempt, the binding-gap analysis, and the next relay are complete. Do not continue inventing additional cognitive machinery.

CREDITS / ARCHITECTURAL LINEAGE

- Christian / Thekidd0329: architecture owner; locus definition, three-variable atom, sparse fruit-bearing-tree rule, topology-driven decay direction.
- Grok: Residual Commitment inversion.
- Gemini: adversarial pressure testing that exposed the stale fuller tuple and novel-locus problem.
- ChatGPT: integration into the current branch schema/spec and relay preparation.

========================================
END RELAY
========================================
