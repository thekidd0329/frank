# `frank.cog` format contract

This file is the persistent image of Frank's native cognitive substrate. The binary format is deliberately independent of Rust struct layout.

## Non-negotiable invariants

1. **Little-endian only.** v1 does not attempt cross-endian mapping. Unsupported endianness is rejected.
2. **Explicit byte encoding.** No `repr(C)` struct is written directly to disk and no Rust struct is blindly memory-mapped as the persistence contract.
3. **The header is trusted first, and nothing else is trusted before it validates.** Magic, sizes, format compatibility, and header CRC must pass before the arena table is interpreted.
4. **The arena table is trusted second.** Its exact expected length and CRC must pass before any arena body is trusted or mapped.
5. **Each arena has an independent schema version.** A cache/projection arena may evolve without forcing the Residual Commitment arena to change format.
6. **Same-major older formats are readable only through code that explicitly understands them.** A newer runtime may project an older arena into the current API without mutating the file.
7. **Older major file formats require an explicit offline migration.** The normal open path does not rewrite a live cognitive image.
8. **Future formats are rejected.** An older runtime never guesses how to interpret newer metadata or arena layouts.
9. **Reserved bytes stay reserved until a format revision assigns them meaning.** Readers ignore them; writers currently emit zeroes.
10. **Generation is monotonic.** Generation counters are reserved for the crash-safe commit protocol; a later slice will define the exact atomic update sequence.
11. **Ground truth is explicit.** The Residual Commitment arena is the authoritative persistent cognitive arena. Symbolic arenas are caches/projections/support and must not silently become independent cognitive truth.
12. **The ground field is sparse.** Zero-force commitments are not durable rows. Empty/neutral loci are represented by absence.

## Ground cognitive record

The current experimental ground atom has exactly three semantic variables:

```text
locus_id          // where
net_force         // signed direction + strength
last_updated_tick // when last materially maintained
```

Candidate A pressure-tests a 16-byte encoding:

```text
bytes 0..8    locus_id          u64 little-endian
bytes 8..12   net_force         raw IEEE-754 f32 bits, little-endian
bytes 12..16  last_updated_tick u32 little-endian
```

This layout is **experimental and not frozen**. In particular, the 32-bit tick is a relative/local clock candidate whose epoch/wrap behavior still needs measurement. The packing target does not outrank the cognitive invariant.

No separate polarity, context binding, flags, persistence class, kind, contestation mass, or inline provenance handle is currently part of the ground atom. Any future field must demonstrate that it carries reconstruction-critical information that cannot be derived or stored in a genuinely auxiliary structure.

## Locus and decay

A locus is the address where residual force is parked in Frank's cognitive space.

It has two intended jobs:

- stable identity for a learned meaning;
- topology/locality for related learned meanings.

The long-term decay rule may read locus topology together with force and age:

```text
decay behavior = function(locus topology, abs(net_force), age)
```

The exact locus addressing/topology scheme is deliberately unresolved so the format does not smuggle in a propositional ontology.

## File metadata v1

The file begins with a fixed 128-byte header containing:

- `FRANKCOG` magic
- format major/minor
- little-endian marker
- file flags
- encoded header size
- encoded arena-descriptor size
- arena count
- creation timestamp
- last-written timestamp
- commit generation
- arena-table offset
- total image length
- CRC32 of the arena descriptor table
- reserved expansion area
- CRC32 of the first 124 header bytes

The descriptor table is normally placed at byte 4096 so the first page leaves room for future superblock/commit metadata without moving every arena.

## Arena descriptor v1

Each descriptor is exactly 64 bytes and contains:

- arena ID
- independent arena schema version
- arena flags
- fixed element size, or zero for variable-width arenas
- capacity
- live count
- byte offset
- byte length
- last committed generation
- reserved bytes

Persistent arena IDs are currently:

```text
1   Entity               rebuildable compatibility/projection cache
2   Relation             rebuildable compatibility/projection cache
3   Episode              rebuildable compatibility/projection cache
4   Goal                 rebuildable compatibility/projection cache
5   Belief               rebuildable compatibility/projection cache
6   Activation           rebuildable working/cache surface
7   TemporalIndex        rebuildable index
8   Provenance           auxiliary evidence/explanation material
9   StringPool           auxiliary representation material
10  FreeList             storage-management support
11  ResidualCommitment   AUTHORITATIVE COGNITIVE GROUND ARENA
```

IDs `1..10` are retained for branch-format continuity rather than renumbered after the Residual Commitment inversion. Retaining an ID does not confer ground-truth status.

`REBUILDABLE_CACHE` may be used to mark persisted projection/cache arenas explicitly.

## Compatibility policy

### File metadata

- same major, file minor <= runtime minor: readable
- older major: **offline migration required**
- newer major: refuse
- newer minor: refuse

### Individual arenas

- exact version: decode normally
- older version: runtime may use an explicit older-version decoder/projector
- newer version: refuse that image with the current runtime

Reading an older arena does **not** imply rewriting it. Migration is a separate operation.

## Migration boundary

Major migrations are intentionally copy/offline operations:

```text
old frank.cog
    ↓ validate completely
migration reader
    ↓ project semantic records
new-format writer
    ↓ validate new image completely
new frank.cog
    ↓ atomic replacement only after success
```

The live cognitive runtime must never partially mutate an old major-format image into a new one during ordinary boot.

## Integrity boundary

The header CRC answers: **can we trust the metadata that tells us where everything is?**

The arena-table CRC answers: **can we trust the descriptors that tell us how the cognitive arenas are laid out?**

Arena body integrity and crash-safe generation commits remain separate design slices; CRC32 alone does not make live persistence atomic.

## What v1 intentionally does not decide

- the final Candidate A field widths or whether 128 bits survives pressure testing
- novel-locus construction and topology encoding
- locus split/merge identity rules
- the final decay equation or topology-to-retention mapping
- relative tick epoch/wrap behavior
- mmap write protocol
- crash-safe superblock commit sequence
- compaction
- body checksums
- concurrency model
- whether provenance is ever reconstruction-critical
- how symbolic projections learn their interpretation rules
- how a neural substrate proposes raw evidence/loci without becoming Frank's cognitive authority

These are research questions. Freezing them before simulation/measurement would recreate the ontology-first mistake this branch exists to avoid.

## Credits

The ground-format direction follows the same lineage documented in the repository README and `docs/residual-commitment.md`: Christian/Thekidd0329 owns the architecture and three-variable/locus/sparse-tree decisions; Grok drove the Residual Commitment inversion; Gemini pressure-tested the atom and novel-locus problem; ChatGPT integrated the resulting repository schema; Claude and Copilot remain downstream stress-test/hole-finding stages.
