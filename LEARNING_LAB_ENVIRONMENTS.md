# Frank Learning Laboratory Environments

This phase is Linux-terminal only. Android, app-store packaging, OS integration, cloud agents, and pretrained language generation are out of scope.

The laboratory is intentionally split into five environments so teaching does not leak into testing and engineering instrumentation does not become developmental experience.

## 1. Work environment

Purpose: humans and Codex build, inspect, debug, and test Frank.

Contains source code, unit tests, instrumentation, phi/control comparisons, logs, and developer tools.

Rule: Frank never learns from developer logs, test labels, stack traces, assertions, filenames, or instrumentation emitted by this environment.

## 2. Learning environment

Purpose: Frank's actual first visual world.

Initial sensory scope is deliberately constrained to rendered webpages and phone interfaces only.

Frank receives sequences of:

- rendered pixel frames;
- temporal ordering;
- actions/events;
- before/after state changes;
- consequence signals that are part of the experience.

He must not receive teacher-side semantic labels such as LOGIN_BUTTON, ACCESSIBILITY_MENU, SUCCESS, DISTRACTOR, TARGET, or FIRST_WORD.

The primitive sequence is:

SCREEN(t0) -> EVENT/ACTION -> SCREEN(t1) -> OUTCOME

Static screenshots alone are insufficient. State transitions must appear early so Frank can learn that interface elements participate in changes.

## 3. Teaching environment

Purpose: arrange developmental experience without writing interpretations into Frank.

The teacher knows which screens contain planned recurrences, distractors, contradictions, and hidden relationships. Frank sees only the permitted experiential stream.

The stream should be information-dense rather than cleanly labeled. It should include menus, dialogs, forms, scrolling, errors, enabled/disabled states, notifications, navigation changes, different layouts, irrelevant motion/change, repeated motifs, false correlations, contradictory examples, and sparse caregiver shaping.

The first recurring visual anchor is a literal pixel image embedded into webpage/phone-interface experience. It is intended to become associated with the pathway Frank is meant to discover around accessibility, but the code MUST NOT label the asset as accessibility or encode its meaning as a rule.

The desired recurring structural pressure is broadly:

current digital path does not achieve the intended outcome -> recurring visual anchor appears within the experience -> an alternate viable interface/pathway becomes available -> intended outcome becomes reachable.

This structure must appear in varied interface contexts so the anchor cannot be learned as one exact screenshot/action pair.

Counterexamples are mandatory. Difference, unusual layout, novelty, or the mere presence of the marker must not always imply intervention or success.

## 4. Test environment

Purpose: determine whether learned structure transfers.

Held-out test assets must never enter the teaching or learning stream.

Tests should use unseen webpages and phone interfaces with changed color, layout, wording, control placement, and visual style while preserving selected latent relationships.

The test must distinguish:

- exact memorization;
- cue association;
- transfer/generalization;
- false-positive salience;
- forgetting;
- restart reconstruction.

Do not repeatedly tune the teaching curriculum against the same held-out test set. Replace/rotate held-out sets when development decisions have been made from their results.

## 5. Observation environment

Purpose: let humans measure Frank without feeding their interpretation back into him.

May expose association strengths, retrieval events, salience changes, residual force, phi/control differences, developmental timeline, and reconstruction state.

Rule: observation output is telemetry for researchers only. It must never enter Frank's sensory stream or become reinforcement.

# No-idle rule

There should be no meaningless idle period while a developmental session is active, but this does not mean infinite external bombardment at maximum rate.

When fresh external experience is not being presented, bounded internal work may occur:

- replay unresolved experiences;
- attempt recall from partial cues;
- rank epistemic gaps;
- consolidate eligible associations;
- weaken stale associations according to the active memory profile;
- compare competing associations;
- form a structural question intent from genuine gaps.

Internal work MUST NOT invent new evidence, fabricate caregiver feedback, create semantic labels, or reward its own guesses.

The scheduler should rotate among high-density experience, distractor/noise, recurrence, contradiction, novel interface, recall opportunity, bounded consolidation, and new experience.

# First-emergence rule

Do not preselect Frank's first concept or first word.

The experiment asks what becomes salient enough to organize around under the teaching environment. If a symbol later begins to emerge, test it against novel held-out interface situations and restart reconstruction before treating it as learned.

No code may contain a shortcut such as `if marker then accessibility`, `if red then say red`, or any equivalent canned semantic mapping.

# Core laboratory principle

The teacher may arrange experience. The teacher may expose consequence. The teacher may provide sparse caregiver shaping. The teacher may not write the interpretation Frank is supposed to discover into Frank's input.
