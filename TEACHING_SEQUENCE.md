# Frank continuous teaching sequence

This protocol is for the Linux-terminal newborn learning phase only.

The goal is not to hand Frank a tiny clean training set or predetermine what his first concept or first word will be. The goal is to expose him to a dense stream of mixed relevance and observe which recurring structures become salient through co-occurrence, consequence, contradiction, novelty, recurrence, and sparse caregiver intervention.

## Core rule

There is no dead idle period while Frank is awake.

Every cycle should be one of:

- new observation;
- paired observation;
- unrelated distractor;
- contradiction;
- reinforcement;
- partial-cue recall attempt;
- novelty probe;
- consolidation/replay;
- gap/question generation when available.

Do not fill time with repeated identical inputs. High information density is useful only when the stream contains variation, decoys, recurrence, interference, and sparse relevance signals.

## Do not predetermine the answer

Do not choose one concept and secretly design the whole stream so that it must win.

Do not hard-code a first word.

Do not give any candidate symbol a privileged semantic type.

Instead, present several latent regularities at once. Some should recur across contexts, some should recur only accidentally, some should conflict, and some should disappear.

The experiment asks: which structures become retrievable and stable enough to organize future recall?

If a symbol eventually emerges, its meaning must be inferred from the experiences that support it. We do not declare what it means in advance.

## Dense-stream starting mix

For an early teaching block, begin around:

- 55-65% unrelated observations / distractors;
- 15-25% recurring non-target patterns;
- 10-15% several different latent regularities distributed across contexts;
- 3-7% caregiver reinforcement/correction;
- 3-7% recall/generalization probes.

These are experimental starting ranges, not truths. CONTROL and PHI must receive identical sequences when compared.

The most frequent token must not automatically be treated as the important one. We want to know whether structure + recurrence + consequence can beat raw repetition.

## Phase 0 — baseline

1. Run the full test suite.
2. Start a fresh isolated journal under `./build/learning-mode/`.
3. Run PHI for the active experiment.
4. Keep a separate clean CONTROL journal for identical-sequence comparison.

## Phase 1 — information flood without language

Feed opaque observations from several independent families.

Example forms:

- `shape-a-color-b-size-c`
- `tone-low-short`
- `motion-left-fast`
- `near-window`
- `warm-surface`
- `object-7-moving`
- random decoys with no useful recurrence.

Do not tell Frank which family matters.

Use `/observe` + `/commit` for ordinary exposure.

Use `/pair A :: B` only when two experiences genuinely co-occur in the teaching event.

Vary order continuously and interleave families. Avoid long clean runs of one category.

## Phase 2 — competing regularities

Introduce multiple learnable structures at the same time.

For example:

- one visual feature recurring across many different objects;
- one spatial relation recurring across several contexts;
- one sound repeatedly co-occurring with a class of events;
- one accidental pattern that appears often early and then disappears;
- one misleading correlation that is later contradicted.

Do not designate a winner.

## Phase 3 — sparse caregiver shaping

Caregiver intervention should be informative but rare.

Reinforce events only when there is a real reason to emphasize them.

Contradict false associations only after recording what Frank retrieved first.

Do not reinforce every occurrence of a pattern. If every meaningful event is labeled or rewarded, the experiment becomes supervised classification instead of developmental salience learning.

The caregiver should influence development without directly writing beliefs.

## Phase 4 — symbols enter the stream

Only after opaque association learning is working, introduce several opaque symbols naturally alongside experience.

Examples might be `ba`, `ko`, `red`, `home`, or any other token, but none receives privileged semantic treatment.

Different symbols should co-occur with different distributed regularities across varied contexts.

The code must not contain rules such as `if feature X then output symbol Y`.

The teaching harness must not tell cognition which symbol is supposed to become the first word.

## Phase 5 — interference and contradiction

Make the environment messy.

Include:

- features that overlap across categories;
- recurring distractors;
- partially misleading correlations;
- changed contexts;
- contradictory examples;
- symbols that sometimes occur near irrelevant events.

A robust association should survive because support is distributed across experience, not because the training stream is trivial.

## Phase 6 — spontaneous candidate detection

Periodically inspect what Frank can retrieve from partial cues.

Do not ask only for a predetermined answer.

Record:

- strongest recalled loci;
- association strengths;
- which symbols, if any, are becoming consistently retrievable;
- whether the same candidate appears across different contexts;
- whether distractors are losing relative influence.

A candidate first symbol is one that begins to recur in retrieval across varied experiences without exact-example lookup.

## Phase 7 — novel-context generalization gate

Once a candidate symbol emerges, create a new experience that was never stored verbatim but contains part of the distributed structure apparently associated with that symbol.

Do not include the candidate symbol in the probe.

Pass condition:

Frank independently retrieves/selects the same candidate symbol from the novel context because of learned distributed associations.

Fail conditions include:

- exact-example lookup;
- hard-coded semantic classification;
- canned response table;
- pretrained language model generating the answer;
- the test harness injecting the expected symbol;
- success only on memorized contexts.

The first-word milestone is not `Frank said the word we wanted`.

It is `Frank produced a symbol whose use is predictably grounded in prior experience and generalizes to a novel case`.

## Phase 8 — restart gate

Exit the teaching terminal completely.

Restart from the same journal.

Present another novel context related to the emergent candidate.

The same grounded relationship must remain available after reconstruction.

## Phase 9 — CONTROL vs PHI

Replay the exact same teaching sequence from a clean state under CONTROL and PHI.

Compare at minimum:

- number of exposures before stable retrieval;
- strongest association distribution;
- forgetting rate;
- interference resistance;
- false-association rate;
- persistence after contradiction;
- generalization success;
- reconstruction after restart.

Do not tune the sequence after seeing which profile wins.

## No-idle behavior

`No idle` does not mean nonstop new sensory input with no processing.

When external input is temporarily absent, use the period for bounded internal work:

1. replay recent unresolved associations;
2. test partial-cue retrieval;
3. rank uncertainty/gaps;
4. consolidate if homeostatic state calls for it;
5. generate a question intent when the self-questioning branch supports it;
6. otherwise inject another externally prepared observation from the teaching stream.

Do not invent facts during internal replay. Internal cycles may reorganize existing evidence but cannot create new external evidence.

## Stop condition for this phase

Stop and report results when either:

A. an emergent symbol passes novel-context generalization and restart reconstruction; or

B. the sequence completes without a grounded symbol emerging.

Failure to produce a first word is still a valid experimental result. Do not force one.