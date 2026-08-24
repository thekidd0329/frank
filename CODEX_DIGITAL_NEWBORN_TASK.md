# Codex task: build Frank's digital newborn environment

Read these first:
- `README.md`
- `docs/PHI_MEMORY_LEARNING.md`
- `CODEX_LEARNING_MODE.md`
- `TEACHING_SEQUENCE.md`
- `docs/DIGITAL_NEWBORN_ENVIRONMENTS.md`

Then inspect the existing visual newborn, teaching journal, association field, control/phi memory profiles, and tests before changing code.

## Immediate implementation

Implement the four-environment contract in `docs/DIGITAL_NEWBORN_ENVIRONMENTS.md` for Linux terminal execution.

Build a deterministic synthetic UI curriculum generator that produces webpage and phone-interface images only. Do not fetch copyrighted/private screenshots from the network. Generate simple interfaces locally from primitives: rectangles, glyph-like marks, fields, toggles, menus, dialogs, navigation bars, cards, disabled states, success/failure states, and alternate pathways.

Create a fixed recurring visual motif as raw pixels. Runtime code and learner-visible manifests must use an opaque ID/hash; do not expose the word `accessibility` or semantic answer labels to Frank.

Generate dense streams containing multiple competing recurring motifs and distractors. The special recurring motif must correlate with alternate viable pathways following blocked/failed interface routes, but it must not be a perfect direct reward predictor. Include counterexamples.

Represent learning as temporal episodes: before frame, opaque action/transition token, after frame, consequence. Reuse the existing visual feature pipeline and developmental association/memory machinery rather than adding a classifier with semantic labels.

Add a continuous developmental scheduler. When no external episode is queued, perform bounded replay, recall probes, comparison, aging/consolidation, gap formation, and checkpointing. Internal cycles must never create observations or reinforcement that did not occur.

Add strict train/test leakage checks based on content hashes. Held-out evaluation must never mutate developmental state or reinforce associations.

Run the identical ordered curriculum under CONTROL and PHI. Do not tune one stream separately.

Add restart/reconstruction tests.

Add a report that prints the strongest emergent loci/associations and transfer behavior without translating opaque loci into human semantic labels inside the learner.

## Required commands

Provide one Linux entry point, preferably `./run-digital-newborn.sh`, that validates the environment, runs baseline tests, runs both profiles, restarts from journals, performs held-out evaluation, and writes a human-readable report.

Provide a teacher entry point that can continuously feed curriculum blocks and inspect state without requiring Android or a GUI.

## Hard prohibitions

Do not add Android.
Do not add a pretrained language model.
Do not add cloud/network dependencies.
Do not hard-code a first word.
Do not implement `if marker then alternate_path` or equivalent answer leakage.
Do not expose evaluator semantic labels to learner code.
Do not treat idle internal processing as new evidence.
Do not delete or weaken existing passing tests to make the new harness pass.

## Stop condition

Stop after the environment, scheduler, curriculum generator, leakage checks, tests, and reports are implemented and the full suite has been run. Report failures honestly. Do not continue into language or Android integration.