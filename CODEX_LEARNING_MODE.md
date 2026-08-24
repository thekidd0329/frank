# Codex task: get Frank into learning mode

This branch is intentionally **Linux-terminal only**. Do not add Android, app-store, APEX, SELinux, networking, UI automation, cloud APIs, or production-platform work here.

## Current state

Frank already has:
- persistent newborn teaching journal;
- Residual Commitment / newborn learning loop;
- CONTROL and phi-derived memory dynamics profiles;
- opaque bidirectional association learning;
- reinforcement, contradiction, passive aging, recall, and restart reconstruction;
- a Linux teaching terminal.

The current phase is **memory -> association -> symbol grounding -> first learned word**.

## Non-negotiable developmental rules

1. Do not hard-code a word response.
2. Do not add `if stimulus == X then say("word")` rules.
3. Do not use a pretrained language model to create the first-word result.
4. Do not assign semantic labels directly into memory.
5. A symbol must be learned from repeated co-occurrence across varied experiences.
6. The first-word test must include a novel cue/context not stored as a canned pair.
7. Passive decay may weaken memory toward uncertainty/zero but must not invert polarity by itself.
8. Learned state must survive process restart through the existing append-only journal/reconstruction path.
9. CONTROL and PHI must receive identical experience sequences for comparison. Do not tune the scenario until PHI wins.
10. Preserve existing tests and architecture invariants.

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
./teach.sh --profile phi --state /tmp/frank-first-word.log
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

Add the smallest platform-neutral **symbol-grounding/generalization experiment** necessary to test whether one opaque symbol can become associated with a shared feature across multiple different contexts.

The test shape should be approximately:

```text
context A + feature R <-> symbol S
context B + feature R <-> symbol S
context C + feature R <-> symbol S

negative/control examples:
context D + feature Q
context E + feature T

NOVEL context F + feature R
        ↓
Frank selects/recalls S
```

The code must not know that `R` means a color, object, word, or human concept. Treat all inputs as opaque developmental loci/features.

## Required acceptance tests

Codex should add deterministic tests proving all of the following:

- repeated varied contexts strengthen a shared symbol association;
- exact-example memorization is insufficient to pass the test;
- a novel context containing the learned shared feature retrieves/selects the learned symbol;
- unrelated features do not retrieve the symbol above threshold;
- passive aging can weaken symbol retrieval;
- reinforcement can restore/strengthen it;
- restart reconstruction preserves the learned generalization state;
- the identical scenario runs under CONTROL and PHI and reports their behavioral differences;
- no first-word output path contains a canned mapping from test stimulus to symbol.

## First-word gate

Do **not** claim first word merely because `/pair X :: S` can recall `S`.

The first-word gate passes only when:

1. Frank receives several different developmental experiences sharing a common latent feature;
2. the same opaque symbol is paired with that shared feature in those varied experiences;
3. Frank later receives a novel experience that was never paired directly with the symbol;
4. Frank selects/produces the symbol because learned structure generalized to the novel experience;
5. the result survives restart;
6. a deterministic test demonstrates that removing the shared learned structure causes the result to fail.

## Scope stop

When the generalization/first-word harness is implemented and all tests pass, stop. Do not continue into Android, speech synthesis, cloud models, UI, or operating-system integration.

Report:
- files changed;
- exact tests added;
- CONTROL vs PHI observations;
- whether the first-word gate passes or what remains missing.
