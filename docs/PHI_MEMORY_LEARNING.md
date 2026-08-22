# Phi memory learning experiment

This phase exists before Android and before language.

The question is not whether phi-derived constants look elegant. The question is whether a phi-derived memory law changes newborn retention, reinforcement, interference, forgetting, and association in a useful and reproducible way.

## Current experimental profiles

`MemoryDynamicsProfile.CONTROL` is a deliberately simple baseline.

`MemoryDynamicsProfile.PHI` derives its values only from inverse powers of the golden ratio. It is an experiment, not a declaration that those values are biologically correct.

Both profiles receive identical developmental experiences. The code must report the behavioral difference rather than tune the scenario until phi wins.

## Learning milestone

Frank reaches the first learning milestone when all of the following are true:

1. repeated co-occurrence between two previously opaque experiences creates a retrievable association;
2. the association strengthens through repetition rather than direct semantic assignment;
3. passive time weakens memory toward zero without reversing polarity;
4. associations can become unretrievable through ordinary forgetting;
5. the append-only teaching journal reconstructs learned associations after restart;
6. the same experience sequence can be run under control and phi profiles for comparison.

This is not yet language. A pair such as `round-red-pattern` and `ba` is intentionally treated as two opaque byte patterns. The test passes because one learned locus can retrieve the other after repeated paired experience, not because the code knows that either pattern is an object or a word.

## Linux terminal

The teaching terminal supports the experiment without Android:

```text
--profile control|phi
/pair A :: B
/recall A
/age N
/associations
```

Repeated `/pair` operations model repeated coupled experience. `/recall` returns opaque loci whose strengths cross the active profile's retrieval threshold. `/age` applies passive forgetting and is journaled so restart reconstruction sees the same developmental history.

## Next gate

Do not jump from association directly to a pretrained language model.

The next developmental gate is symbol grounding: demonstrate that a symbol paired across varied sensory situations can later be selected or produced because of its learned associations, including a novel cue/context that was not stored as a canned response.

Only after that should work proceed toward first-word and talking-state tests.
