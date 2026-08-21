# F.R.A.N.K. goal-control loop

F.R.A.N.K. is not centered on conversation or on a queue of commands. Conversation is one input into a persistent goal-control system.

The control loop is:

1. represent the owner's intent as a `Goal` with an explicit desired state and success criteria;
2. observe a `WorldState`;
3. propose candidate actions that can move the world toward the goal;
4. evaluate factual certainty and source exhaustiveness;
5. evaluate likely consequences and reversibility;
6. choose between `ACT`, `WARN_AND_ACT`, and `ASK`;
7. execute through a platform/runtime capability;
8. observe the resulting world state;
9. verify whether success criteria changed;
10. continue, re-plan, or finish based on the observed result rather than on action count.

## Why this is different from a command assistant

The old center of gravity was effectively:

`input -> task -> next action -> execute -> next action`

The new center is:

`goal <-> world model -> judgment -> action -> observation -> verification -> goal`

An action is useful only if it changes the world in a way that advances the desired outcome.

## Consequence-aware autonomy

Confidence answers whether F.R.A.N.K. believes it understands the target, facts, and intent. It does not answer whether an action is wise.

`AutonomyEvaluator` therefore accepts both epistemic risk dimensions and consequence risks. A sufficiently known, materially consequential but reversible action can be classified `WARN_AND_ACT`: F.R.A.N.K. may proceed while surfacing the cost. Severe irreversible consequences or insufficient certainty produce `ASK`.

This is deliberately not a universal morality table. It is a control primitive for the JARVIS-like behavior the project is aiming at: understand the goal, predict what an action will cause, act when appropriate, and interrupt only when the uncertainty or consequence genuinely matters.

## Android boundary

This package is platform-independent Kotlin. Android integration should implement observation and execution around it rather than move Android semantics into the goal model.

The next platform layer should provide:

- durable goal restoration after process death/reboot;
- richer device/world observations;
- capability providers for Android actions;
- effect verification after actions;
- temporary worker-agent delegation for bounded subproblems;
- a privileged authority strategy appropriate to the owner-controlled device tier.

The goal-control package should remain the reasoning/control contract while Android becomes F.R.A.N.K.'s eyes and hands.
