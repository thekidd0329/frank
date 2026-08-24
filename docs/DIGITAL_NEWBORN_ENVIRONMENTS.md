# Frank Digital Newborn Environments

## Scope

First-breath remains Linux-terminal only. Frank's visual world is constrained to webpages and phone interfaces. Android integration, cloud models, pretrained language generation, and real device authority remain out of scope.

The purpose of this phase is not to manufacture a desired first word. It is to expose Frank to a dense digital environment, let salience compete, and measure what is actually learned.

## Four isolated environments

### 1. Work environment

`workspace/`

This is the development/operator area. It contains launch scripts, manifests, run logs, reproducibility metadata, and human-readable reports. It must never be used as Frank's developmental memory.

A run receives a unique run ID, profile (`control` or `phi`), deterministic seed when requested, dataset version, and journal path.

### 2. Test environment

`testbed/`

Tests are sealed from teaching. Test scenes may use the same interface grammar but must not reuse teaching screenshots byte-for-byte.

The testbed measures:
- visual discrimination;
- recurrence detection;
- association strength;
- forgetting without polarity inversion;
- restart reconstruction;
- novel-context transfer;
- salience ranking;
- false-positive response to distractors;
- control versus phi behavior under identical streams.

A held-out test must never reinforce Frank. Evaluation is observation only.

### 3. Learning environment

`learning/`

This is Frank's experienced stream. Input consists strictly of webpage and phone-interface imagery or deterministic synthetic renders of those interfaces.

Each experience is represented as a temporal episode rather than a semantic label:

`frame -> transition/action token -> frame -> consequence`

The learning system receives pixels/features, temporal ordering, recurrence, and consequence. It does not receive hidden curriculum annotations such as `accessibility`, `important`, `button`, or `correct answer`.

The stream should be information-dense. Many patterns recur. Most are irrelevant or weakly relevant. Some predict transitions. Some predict failure. Some predict successful alternate pathways. Salience must emerge through recurrence, prediction, consequence, caregiver shaping, and competition rather than a privileged label.

### 4. Teaching environment

`teaching/`

The teacher controls curriculum presentation but does not directly write semantic beliefs.

Teacher operations are limited to:
- present an episode;
- repeat or vary an episode;
- reinforce an experienced outcome;
- contradict an experienced inference;
- introduce a new context;
- withhold reinforcement;
- request recall/projection;
- trigger bounded consolidation;
- record observations about behavior.

The teacher must not inject `meaning = X` or hard-code a first word.

## Recurring visual pathway marker

The curriculum contains one stable recurring bitmap/pixel motif associated with the *pathway* around inaccessible or failed digital interactions. It is not named `accessibility` in any runtime-visible data.

The marker must appear across visually different webpage and phone-interface episodes. Its surroundings, layout, controls, colors, text-like glyphs, and task geometry vary. What remains statistically useful is its relationship to discovering or selecting an alternate viable digital pathway after a blocked/failed route.

Counterexamples are mandatory. The marker must not become a trivial `marker -> reward` switch, and visual difference alone must not imply intervention.

The held-out test asks whether the learned structure transfers to a novel interface configuration. The system is scored on behavior/association, not on whether it emits the English word `accessibility`.

Only after a stable pre-language structure exists may a human symbol be paired with it.

## No-idle rule

No idle period means no developmental dead zone; it does not mean inventing evidence.

When external teaching input is unavailable, Frank cycles through bounded internal operations:
1. replay recent unresolved episodes;
2. attempt partial-cue recall;
3. compare competing associations;
4. age/decay according to the active memory profile;
5. consolidate supported recurrence;
6. identify high-uncertainty/high-tension gaps;
7. form a question/request intent for future caregiver input;
8. checkpoint durable state.

Internal cycles may reorganize, weaken, strengthen only where the existing learning law permits, or expose uncertainty. They may never fabricate a new observation or caregiver reinforcement.

## Information-density rule

Do not create a curriculum where one signal is obviously special. Every teaching block should contain competing recurring patterns, distractors, benign repetitions, changing layouts, and unrelated transitions. The target pathway marker is only one regularity among many.

Curriculum annotations used by the harness for scoring must be inaccessible to the learning code.

## Leakage controls

Runtime-visible IDs are opaque hashes/indices. Filenames and manifests consumed by Frank may not contain semantic answers. Ground-truth labels live only in evaluator-side files that the learner cannot open.

Teaching and held-out test assets are separated by directory and manifest. Tests should fail if a test asset hash occurs in the teaching manifest.

## Required directory contract

```text
newborn-env/
  workspace/
    runs/
    reports/
  learning/
    assets/
    episodes/
    manifests/
  teaching/
    curricula/
    sessions/
  testbed/
    heldout/
    manifests/
    reports/
  state/
    journals/
    checkpoints/
```

Generated state and reports should be gitignored; deterministic curriculum definitions and tiny synthetic fixtures may be versioned.

## First gate

The environment is ready when one command can:
1. validate leakage boundaries;
2. run the existing baseline suite;
3. generate/load a deterministic UI-only curriculum;
4. teach under CONTROL and PHI using identical ordered experiences;
5. restart from the journal;
6. run held-out evaluation without reinforcement;
7. report what became salient and whether any learned structure transferred.

There is deliberately no required first word. We report what happened.