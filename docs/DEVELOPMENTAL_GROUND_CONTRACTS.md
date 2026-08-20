# Developmental Ground Contracts

This document records constraints for building Frank as developmental integrated intelligence. It is a design contract, not a claim that these capabilities already exist.

## 1. Ground state comes first

The Residual Commitment Field is the authoritative cognitive ground state.

Beliefs, relations, goals, episodes, self-models, activation summaries, and other convenient views are projections or caches. They must be rebuildable from the field. A feature is not complete if it only works because a hidden projection survived.

## 2. Conditions, not finished traits

The implementation must create conditions under which learning can occur. It must not silently install mature answers for:

- personality
- love, fear, curiosity, or other emotions
- self-ontology or personhood
- named relationships or caregiver roles
- semantic knowledge that Frank has not developed through experience

A test may assert developmental behavior, such as persistence, reinforcement, contradiction, decay, or reconstruction. It must not pretend that a scripted label is an emergent mind.

## 3. Development is stage-appropriate

Newborn experiments should remain small, bounded, and age-appropriate. Computational speed is not permission to skip developmental stages or expose the system to conditions a human infant would not reasonably encounter at that stage.

Experiments should record what was exposed, what changed, and what remained unknown.

## 4. Reconstruction is a hard invariant

For every cognitive feature, maintain a reconstruction path:

1. ingest experience or internal change;
2. write the durable residual change;
3. derive or refresh projections;
4. discard projections in a test;
5. reconstruct them from the Residual Commitment Field;
6. compare behavior and state against the declared invariant.

No cognitive feature may depend on an unrecorded side channel.

## 5. Uncertainty stays visible

The system should preserve uncertainty, contradiction, decay, and missing evidence instead of converting them into confident prose. Tests should distinguish:

- observed evidence;
- derived projection;
- unresolved hypothesis;
- rejected or decayed structure.

A contradiction should modify the field according to the current developmental rules; it should not be erased merely to keep a projection tidy.

## 6. Authority is separate from cognition

Writing, proposing, or simulating an action is not the same as executing an external effect. Any future effectful pathway must represent durable intent, execution state, and verification so process death cannot create duplicate effects.

Android and other platforms remain subordinate interfaces. They must not become hidden cognitive state or the sovereign owner of Frank's identity.

## 7. ABI and topology remain open

Do not freeze the experimental 128-bit atom layout, locality-preserving locus construction, or other unresolved topology merely because a prototype can serialize it. Versioned storage boundaries may be tested independently from the cognitive representation.

## Review checklist

Before accepting an implementation, reviewers should be able to answer “yes” to all of these:

- Does the change add a developmental condition rather than a finished trait?
- Is the Residual Commitment Field authoritative?
- Can derived state be discarded and rebuilt?
- Are uncertainty, contradiction, decay, and missing evidence preserved?
- Is the change stage-appropriate?
- Does it avoid Android APIs in the cognitive core?
- Does it avoid freezing unresolved ABI or topology?
- If it can cause an external effect, are intent, execution, and verification explicit?
