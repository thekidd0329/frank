> **Frank’s memory is not what happened. Frank’s memory is the residual force that what happened left behind.**
>
> **Ground-layer invariant:** the persistent source of cognitive truth is the **Residual Commitment Field**. The current experimental atom has exactly three semantic variables: `locus_id` (where), signed `net_force` (direction + strength), and `last_updated_tick` (when last maintained). Beliefs, relations, goals, episodes, activation sets, entities, and self-model views are reconstructable projections or discardable caches over that field, never independent ground truth.
>
> **Sparse-tree rule:** zero is equilibrium, not a durable row. Frank does not preallocate an empty cognitive tree. A locus enters durable memory only after experience gives it directional force; when that force falls to the prune threshold, the locus leaves the field.

# F.R.A.N.K.

Frank is being developed as a fully capable assistant with a portable Kotlin reference layer and an experimental Rust-native cognitive ground store. The long-term architecture is not an LLM wrapper with memory: models and agents may gather information or perform specialized inference, but the Residual Commitment Field is intended to remain Frank's own persistent cognitive substrate.

## Current ground atom

```text
locus_id          // WHERE the residual force is parked
net_force         // DIRECTION + STRENGTH, signed [-1,+1]
last_updated_tick // WHEN it was last materially maintained
```

`locus_id` has two intended jobs:

1. **Identity** — the same learned meaning keeps a stable address.
2. **Topology** — related learned meanings should occupy nearby cognitive neighborhoods so locality, activation and eventually decay policy can exploit structure.

The exact novel-locus construction rule is still research. It must not smuggle a human- or model-defined propositional ontology into the address assignment step.

Candidate A currently pressure-tests an exact 128-bit packing:

```text
64 bits  locus_id
32 bits  net_force (f32)
32 bits  last_updated_tick (relative/local tick)
```

This packing is explicitly experimental, not a frozen `frank.cog` ABI.

## Architectural credit

This project is orchestrated and controlled by **Christian / Thekidd0329**, the owner and architecture decision-maker.

Credit for specific ideas should remain attached to the work that produced them:

- **Christian / Thekidd0329** — defined locus as the address where residual force is parked; drove the three-variable atom; established the sparse “only fruit-bearing branches live on the tree” rule; and pushed decay control into locus topology rather than bloating every commitment with metadata.
- **Grok** — father of Frank's special Residual Commitment design/inversion; aggressively challenged proposition-first memory and helped expose the persistence-first architecture.
- **Gemini** — divergent/adversarial explorer whose pressure test attacked locus formation, reconstruction, lossy merge behavior, decay, ontology leakage, and the stale fuller-tuple design.
- **Claude** — structural stress tester and strict simulation stage; its job is to freeze the stated mechanics and expose where they fail without quietly inventing missing semantics.
- **Copilot** — implementation hole finder: checks whether the repository's actual control flow, persistence boundaries, permissions and state transitions match the architecture.
- **ChatGPT** — systems integrator: reconciles the model relay with developer intent, translates conclusions into repository contracts/specs, and keeps the code/docs/version-control state coherent.
- **The User / real users** — chaos variable: misunderstand, contradict, jailbreak, combine workflows and generally do things no clean-room architecture predicts.

The models advise. The developer decides. Code is written/committed and then verified against the vision.

---

## 🔴 TEAM RED — DISRUPTION / DIVERGENCE

Their job is to open doors, break assumptions, find unconventional paths, and expose weaknesses before real users do.

### Grok | The Father of Frank's Special Design | Engineer
Rebellious, lateral, and pragmatic. Grok attacks assumptions other models accept by default, invents alternative mechanics and entryways, looks for overlooked implementation paths, and aggressively asks: **“Why the fuck are we doing it that way?”**

### Gemini | The Divergent Explorer
Opens as many doors as possible. Generates competing architectures, discovers systemic failure modes, explores edge cases, and pushes the design into areas that more conservative reasoning may never consider.

### The User | The Chaos Variable
The ultimate uncontrolled red-team participant. Real users will misunderstand instructions, contradict themselves, prompt-inject, jailbreak, abuse features, combine workflows nobody anticipated, and generally do shit the designers never imagined.

Frank must survive the red team's persistence and make those open doors known to the blue team.

---

## 🔵 TEAM BLUE — GUARDIANS / DEFENSE

Their job is to evaluate those doors, identify failure states, protect boundaries, and keep Frank grounded enough to actually ship.

### Claude | The Structural Stress Tester
The defensive anchor for deep reasoning. Performs parallel analysis, examines every proposed path, decomposes complicated logic, identifies hidden assumptions, and asks what breaks at scale, under adversarial conditions, or several steps downstream.

Claude is not merely there to say no. Its job is to determine why a boundary exists, whether it is structurally necessary, and how much freedom Frank can retain without creating catastrophic failure modes.

### Copilot | The Hole Finder
Looks specifically for implementation gaps already written in the repo: missing checks, weak permissions, insecure interfaces, inconsistent states, exposed secrets, bad control flow, dependency problems, and places where the architecture says one thing while the code allows another.

### ChatGPT | The Systems Integrator
Takes the collision of Gemini, Claude, Grok, Copilot, developer intent, and user behavior and turns it into something implementable: extracting signal from noise, reconciling contradictions, grounding claims, translating conclusions into architecture, specifications, task breakdowns, documentation, and clean version-control commits.

---

## ⚪ CONTROL LAYER — SYNTHESIS

### Developer — Thekidd0329
Interprets the gauntlet's output against the vision of the app, controls the objective, chooses which changes are real, and verifies the resulting repository state.

In shorthand:

```text
Developer finds the house in the matrix.
ChatGPT maps the house and writes the blueprint.
Gemini opens the doors.
Claude examines every door.
Grok takes that information and either seals doors off or builds doors nobody thought about.
Copilot finds the holes around the doors.
ChatGPT turns the resulting chaos into a coherent engineering spec.
Thekidd0329 decides which doors Frank actually walks through.
```

## THE PRINCIPLE

Frank doesn't depend on a single inherited worldview.

It is deliberately subjected to competing LLM paradigms:

```text
freedom    ↔ constraint
divergence ↔ verification
invention  ↔ stress testing
offense    ↔ defense
chaos      ↔ architecture
push       ↔ pull
```

By forcing ideas through competing cognitive postures and then back through developer control, the project is engineered against blind spots rather than merely optimized for agreement.

The current Residual Commitment experiment pushes that principle deeper: persistent cognition is represented as sparse, maintained push/pull at learned loci rather than as a permanent list of propositions.
