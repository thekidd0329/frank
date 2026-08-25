# Biological Stack Test Plan

## Purpose

Test whether the stacked architecture behaves like the intended biological processes without relying on duplicated state or conventional software-memory shortcuts.

## A. Working-memory persistence

### A1 Active maintenance
Present a repeated stimulus long enough to maintain an active ensemble. Remove input and measure the decay trajectory.

Expected: active state weakens rather than being synchronously deleted.

### A2 Activity-silent maintenance
Drive an ensemble, suppress firing while leaving recently altered trace state intact, then apply a weak matching cue.

Expected: matching cue reinstates the ensemble faster than an unrelated cue.

### A3 Competitor suppression
Drive two partially overlapping ensembles and inhibit one locally.

Expected: suppressed ensemble remains recoverable if its lower-layer trace state has not been erased.

## B. Opponent representation

Use two nonnegative opponent pathways at the C-phi-plus layer and compare their reconstructed higher-layer signed projection with the existing Kotlin signed residual reference.

Control configuration:
- equal decay on both pathways
- no extra inhibition
- no phi-floor-specific perturbation in the algebraic-equivalence control

Expected control: `positive_path - opponent_path` reproduces the signed reference trajectory.

Then reintroduce biological mechanisms separately:
- retained dormant floor
- local inhibition
- refractory gating
- compartment effects

Measure which mechanism changes transition timing and whether the change improves or harms later reconstruction accuracy.

## C. Tagging without premature long-term storage

Create traces with matched initial activation but different later outcomes:

1. repeated/replayed
2. unrepeated
3. contradicted
4. contextually reinstated
5. strong but one-shot

Expected: initial strength alone must not guarantee durable promotion.

## D. Consolidation

### D1 Replay dependence
Compare tagged traces with and without offline replay.

Expected: replayed traces become more resistant to later interference.

### D2 Reconstruction dependence
Interrupt working state before consolidation and later provide context cue.

Expected: traces that repeatedly reconstruct successfully gain consolidation eligibility.

### D3 Transformation
Compare active representation before consolidation with durable representation afterward.

Expected: durable state need not be byte-identical to transient state; it must preserve behaviorally relevant structure.

## E. Long-term-memory write test

No direct `save()` call is allowed.

A candidate durable commitment must arise only through:

`activation -> persistence/tag -> replay/reconstruction -> consolidation -> Residual Commitment`

Test failure if durable state appears without the required lower-layer history.

## F. Retrieval

Delete transient activation and semantic projections while retaining durable commitments.

Apply a partial contextual cue.

Expected:
- durable state biases/reseeds the relevant lower-layer ensemble
- lower-layer ensemble becomes reconstructable
- semantic projection reappears

Failure if retrieval is implemented as simply reading a finished semantic object from durable storage.

## G. Destruction tests

At each checkpoint independently delete:

- semantic projections only
- working activation only
- consolidation tags only
- all derived state above the Residual Commitment Field

Measure what can and cannot be lawfully reconstructed.

## H. Context-specific recall

Train X in context A and Y in context B. Suppress the branch/path associated with A while leaving B intact.

Expected: X recall collapses selectively while Y remains available.

This tests whether context is physically/local structurally bound rather than stored only as a global label.

## I. Catastrophic interference

Train many traces, allow most to become dormant, then train a new competing set.

Expected: new learning changes accessibility without requiring old traces to become literal zero/deleted state. Reintroducing an old context should recover a measurable subset.

## J. Phi-specificity protocol

Only after a behavior has been specified independently:

1. freeze stimulus schedule
2. freeze scoring rules
3. freeze Kotlin/reference implementation
4. run C-phi-plus at canonical phi
5. perturb the phi-derived constants while preserving all other mechanisms
6. compare reconstruction accuracy, reactivation latency, interference resistance, false-recall rate, saturation, and stability

Do not tune the stimulus after viewing a phi result.

## K. Required metrics

- reconstruction correctness
- transition tick
- reactivation latency
- false reactivation rate
- retained-trace count
- active/dormant ratio
- interference loss
- context-selectivity score
- consolidation survival
- deterministic hash across repeated takes

## L. Triple-run standard

Every canonical experiment runs at least three complete deterministic takes. Any stochastic experiment must use recorded seeds and report distributions rather than a single trajectory.
