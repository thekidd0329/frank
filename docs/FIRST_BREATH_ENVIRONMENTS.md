# Frank First-Breath Environments

This phase is Linux-terminal-only. Android integration, cloud models, pretrained language generation, and app-store/product polish are out of scope.

Frank's visual world for this phase is strictly digital interfaces: webpages and phone interfaces. The purpose is to test whether memory, salience, association, transition learning, and later symbol grounding emerge from experience rather than canned semantic assignments.

## Shared invariant

All four environments use the same production cognition and memory code. Test and teaching code may control inputs, timing, hidden ground truth, and scoring, but must not insert meanings directly into Frank's state.

Never encode `marker == accessibility`, `screen == login`, `button == continue`, or a canned first word inside cognition. Hidden labels may exist only in the evaluator so humans can score behavior.

## 1. Work environment

The work environment is where Frank runs continuously during first breath.

Inputs are ordered interface frames and transition events. A frame is a real or synthetic webpage/phone-interface image presented as pixels through the existing visual pipeline. A transition records before-frame -> attempted interaction/event -> after-frame, without attaching human semantic meaning to the pixels.

The work loop must always have bounded work available:

1. ingest the next interface frame/transition if external teaching input exists;
2. update visual prototypes and residual/association state;
3. attempt recall/prediction from current cues;
4. rank unresolved or competing associations;
5. perform bounded replay/consolidation when no new external item is queued;
6. checkpoint durable state;
7. repeat.

No-idle means no dead developmental gap while the process is running. It does NOT permit inventing observations. Internal cycles may replay, compare, decay, consolidate, predict, or form question/gap intents using already observed evidence only.

## 2. Learning environment

The learning stream is deliberately information-dense. Frank should have to discover what matters rather than receiving a clean curriculum where every presented feature is relevant.

Each episode mixes:
- webpage and phone-interface frames;
- visual distractors that recur but predict nothing useful;
- rare features;
- common features;
- state changes;
- failed transitions;
- successful transitions;
- contradictory/noisy examples;
- one or more recurring visual anchors;
- multiple competing regularities.

The recurring accessibility-pathway image is a literal pixel pattern. It is NOT named or semantically tagged in Frank's input. It recurs around interface sequences in which an initially blocked/unusable path is followed by an alternate interface pathway and a successful task transition. Other recurring images must also exist so frequency alone cannot solve the experiment.

The evaluator may know which image is the accessibility-pathway anchor. Frank may not.

Do not predetermine which concept becomes Frank's first grounded concept or which token becomes his first learned word. Accessibility is a deliberately recurring pathway candidate, not a hard-coded required answer.

## 3. Teaching environment

The teaching environment controls exposure without supplying definitions.

A teaching episode should contain:

```
context frames
-> noise/distractors
-> current interface state
-> transition attempt
-> resulting state
-> sparse caregiver signal when appropriate
-> additional unrelated information
```

Caregiver intervention is sparse. Repetition and consequence must do most of the work. Reinforcement may emphasize that an observed transition mattered, but must not inject the evaluator's semantic label.

Teaching continuously varies irrelevant properties: layout, colors, text density, viewport, component placement, surrounding controls, and unrelated recurring motifs. The accessibility anchor must appear across sufficiently different interface contexts that memorizing one screenshot cannot pass.

Counterexamples are mandatory:
- anchor appears but no useful alternate path follows;
- alternate path succeeds without the anchor;
- visually similar distractor appears;
- ordinary navigation succeeds without any blockage;
- repeated high-frequency motif has no predictive consequence.

These prevent `most frequent pixels == important` and `anchor == guaranteed reward` shortcuts.

## 4. Test environment

Testing is isolated from teaching state mutation unless a test explicitly evaluates online adaptation.

Maintain four suites:

### Memory gate
- repetition changes association strength;
- passive aging weakens without polarity inversion;
- recall crosses/falls below profile thresholds as expected;
- restart reconstructs the same learned field from the journal.

### Salience gate
- a useful recurring structure outranks equally/more frequent irrelevant motifs;
- sparse caregiver emphasis changes priority measurably without directly assigning meaning;
- contradiction/noise does not collapse the field into a canned winner.

### Transition gate
- Frank distinguishes interface frames from state transitions;
- learned expectations can predict/select among possible next states better than chance;
- failure/success history changes later pathway preference.

### Novel-context/generalization gate
Hold out interface families, layouts, and exact screenshots from teaching. Present a novel blocked-path sequence and measure whether learned state increases preference/search toward a viable alternate pathway. The exact accessibility anchor may be present in one test and absent in another to determine whether Frank learned only the marker or a broader transition structure.

A first-word test comes later. A symbol/token may be introduced only after pre-language structure is measurable. The evaluator must not choose the winning token by hard-coded mapping. Whatever token/concept appears must be tested on held-out contexts and after restart.

## Dataset layout

Use this repository shape for generated/curated first-breath data:

```
first-breath/
  work/
    state/
    journals/
    checkpoints/
  learning/
    train/
      web/
      phone/
    anchors/
    distractors/
  teaching/
    sequences/
    caregiver-events/
  test/
    memory/
    salience/
    transitions/
    held-out-web/
    held-out-phone/
  reports/
```

Do not commit private user screenshots, credentials, messages, contact data, or other sensitive material. Initial fixtures should be synthetic/local test interfaces or deliberately curated non-sensitive interface captures.

## Reproducibility

Every generated teaching run records a seed and an append-only event manifest. CONTROL and PHI profiles receive the identical ordered stream when compared. Randomization may vary between experiments, but never between profiles inside the same paired experiment.

Reports must include failures and null results. Do not tune the teaching stream until PHI wins.

## Stop condition for this phase

Do not move to Android or conversational language merely because the terminal looks active.

First breath advances only when the test environment demonstrates, reproducibly:

`pixels -> persistent memory change -> salience -> association -> transition expectation -> held-out generalization`

with no semantic shortcut in cognition.