# Codex task: build Frank's first-breath environments

Read, in order:

1. `README.md`
2. `docs/PHI_MEMORY_LEARNING.md`
3. `CODEX_LEARNING_MODE.md`
4. `TEACHING_SEQUENCE.md`
5. `docs/FIRST_BREATH_ENVIRONMENTS.md`

Then run `./test.sh` before changing code.

## Immediate implementation task

Build the smallest runnable Linux-terminal harness that implements the four environments in `docs/FIRST_BREATH_ENVIRONMENTS.md` using the existing Kotlin cognition/memory and visual-newborn code.

Required deliverables:

- deterministic interface-fixture generator or local fixture loader for webpage/phone-interface pixel frames;
- episode/event manifest with seed, frame identity, transition identity, and evaluator-only hidden metadata kept outside cognition;
- continuous work loop with external-ingest and bounded internal-work modes;
- teaching sequencer that mixes signal, distractors, transitions, counterexamples, and sparse caregiver events;
- train/test split that prevents exact-frame leakage;
- evaluator for memory, salience, transition, and held-out generalization gates;
- CONTROL-vs-PHI paired runner receiving byte-for-byte/equivalent ordered experiences;
- append-only run report containing parameters, seed, scores, failures, and restart result;
- terminal commands/scripts to run one teaching session and one complete evaluation.

## Critical anti-cheating rules

Production cognition may never receive evaluator labels such as `accessibility`, `blocked`, `success`, `button`, `login`, or a predetermined first word unless/ until those symbols are explicitly introduced later as ordinary teaching inputs.

Do not add conditionals that recognize fixture IDs, filenames, pixel hashes, or the accessibility anchor and choose an expected action.

The evaluator may know hidden ground truth. Frank may see only the same sensory/event information allowed by the experiment.

The accessibility-pathway anchor is a recurring image, not a command opcode. Include other recurring motifs and counterexamples so recurrence alone is insufficient.

Do not use a pretrained LLM, OCR, accessibility tree, DOM semantics, Android APIs, cloud services, or external semantic classifiers for first breath. This phase is pixels + transitions + existing developmental state.

## No-idle rule

While a run is active, every cycle does one bounded unit of external or internal work. Internal work can replay, recall, compare, age/consolidate, predict, rank unresolved associations, or checkpoint. It cannot fabricate evidence.

The loop must remain interruptible and deterministic under a recorded seed. Do not implement a busy-spin that consumes CPU merely to satisfy `no idle`.

## First milestone

Do not implement conversational output yet. Stop when a reproducible run can demonstrate or falsify:

`interface pixels -> persistent memory -> learned salience -> transition association -> held-out generalization`

Run the full test suite afterward and report the actual results, including failures. Do not tune constants merely to force a passing PHI result.