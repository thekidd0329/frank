# Codex task: get Frank into learning mode

This branch is intentionally **Linux-terminal only**. Do not add Android, app-store, APEX, SELinux, networking, UI automation, cloud APIs, or production-platform work here.

Before changing code, read these files completely:

1. `READ_ME_FIRST.txt`
2. `CODEX_LEARNING_MODE.md`
3. `TEACHING_SEQUENCE.md`
4. `LEARNING_LAB_ENVIRONMENTS.md`
5. `docs/PHI_MEMORY_LEARNING.md`
6. `docs/NEWBORN_PRIMITIVE.md`

## Current state

Frank already has:
- persistent newborn teaching journal;
- Residual Commitment / newborn learning loop;
- CONTROL and phi-derived memory dynamics profiles;
- opaque bidirectional association learning;
- reinforcement, contradiction, passive aging, recall, and restart reconstruction;
- a Linux teaching terminal.

The current phase is **memory -> association -> salience -> generalization -> emergent symbol/first learned word**.

The first concept/word MUST NOT be chosen in advance. The experiment asks what becomes salient enough to organize around under sustained high-density experience.

## Non-negotiable developmental rules

1. Do not hard-code a word response.
2. Do not add `if stimulus == X then say("word")` rules.
3. Do not use a pretrained language model to create the first-word result.
4. Do not assign semantic labels directly into memory.
5. Do not preselect Frank's first concept or first word.
6. Symbols must be learned from repeated structure across varied experience, not exact-pair memorization.
7. Held-out tests must use novel cues/contexts not stored as canned pairs.
8. Passive decay may weaken memory toward uncertainty/zero but must not invert polarity by itself.
9. Learned state must survive process restart through the existing append-only journal/reconstruction path.
10. CONTROL and PHI must receive identical experience sequences for comparison. Do not tune the scenario until PHI wins.
11. Preserve existing tests and architecture invariants.
12. Frank's first visual world is restricted to rendered webpages and phone interfaces.
13. Teacher-side labels, targets, hidden recurrence metadata, and test answers must never enter Frank's sensory stream.
14. There is no meaningless idle period during an active developmental session. When no fresh external experience is available, run bounded replay/recall/gap/consolidation work without inventing new evidence or self-reward.

## First command

Run the full suite before changing anything:

```bash
./test.sh
```

Then run the phi teaching terminal:

```bash
./teach.sh --profile phi
```

For an isolated disposable journal during experimentation:

```bash
./teach.sh --profile phi --state /tmp/frank-learning.log
```

Or use the wrapper:

```bash
./run-learning-mode.sh
```

## Existing teaching commands

```text
/observe <raw text>
/commit
/reinforce [0..1]
/contradict [0..1]
/pair A :: B
/recall <cue>
/age <steps>
/recover <0..1>
/state
/field
/associations
/history
/quit
```

## Immediate implementation task

Build the smallest platform-neutral teaching/learning harness that can continuously present an information-dense sequence of **webpage and phone-interface pixel experiences** while preserving strict separation between work, teaching, learning, observation, and held-out test environments.

The sequence must contain:

- many unrelated interface images and state transitions;
- recurring and competing visual structures;
- distractors and false correlations;
- contradictions;
- sparse caregiver shaping;
- before/action/after/outcome sequences;
- a literal recurring pixel image/visual anchor that appears across varied interface contexts;
- held-out novel interfaces that never enter training.

The recurring anchor is intended by the researchers to participate in the pathway around accessibility/alternate viable digital paths, but **that semantic meaning must not be encoded into Frank's inputs, memory, filenames visible to cognition, response rules, or tests**.

Do not implement a shortcut such as:

```text
marker -> accessibility
marker -> alternate path
if marker then choose X
```

The purpose is to see what Frank actually makes salient under the arranged experience.

## No-idle scheduler requirement

An active developmental run should rotate among:

```text
external high-density experience
-> distractor/noise
-> recurrence
-> contradiction
-> novel interface
-> recall opportunity
-> bounded internal consolidation/replay
-> next external experience
```

Internal periods may replay unresolved associations, test recall from partial cues, rank genuine gaps, consolidate, weaken stale associations under the active memory profile, and form structural question intent. They may NOT create new evidence, fabricate teacher approval, attach semantic labels, or reward Frank's own guesses.

## Required acceptance tests

Codex should add deterministic tests proving all of the following:

- teacher-only metadata cannot enter Frank's learning input;
- held-out test assets cannot be read by the training scheduler;
- repeated visual recurrence can alter association/salience without a hard-coded semantic mapping;
- distractors and false correlations do not automatically dominate;
- counterexamples can weaken an incorrect simple association;
- novel held-out interface contexts can test transfer independently of exact screenshot memorization;
- passive aging can weaken retrieval;
- reinforcement can restore/strengthen learned structure;
- restart reconstruction preserves learned state;
- the identical developmental sequence runs under CONTROL and PHI and reports behavioral differences;
- observation telemetry is one-way and cannot feed back into learning;
- no-idle internal work cannot invent new evidence;
- no output path contains a canned mapping from a recurring visual marker to accessibility, a chosen word, or a chosen action.

## Emergence / first-word gate

Do **not** claim a first word merely because an exact `/pair X :: S` recalls `S`.

Do not choose the first word before the run.

A candidate symbol/word becomes interesting only if:

1. it emerges from repeated structure in varied developmental experiences;
2. it becomes selectively associated with a latent relationship rather than one exact image;
3. it transfers to a novel held-out interface/context;
4. unrelated/negative examples do not trigger it indiscriminately;
5. the result survives restart;
6. removing the learned shared structure makes the behavior disappear;
7. there is no programmer-supplied semantic lookup that can explain the result.

If no symbol emerges, report that honestly. The experiment is allowed to fail or produce something unexpected.

## Scope stop

When the continuous teaching/learning environment, held-out transfer harness, and emergence measurement are implemented and all tests pass, stop. Do not continue into Android, speech synthesis, cloud models, UI automation, or operating-system integration.

Report:
- files changed;
- exact tests added;
- how the five environments are isolated;
- how the no-idle scheduler works;
- CONTROL vs PHI observations;
- what, if anything, emerged;
- whether the learning/generalization gate passes or what remains missing.
