# `frank.cog` format contract

This file is the persistent image of Frank's native cognitive substrate. The binary format is deliberately independent of Rust struct layout.

## Non-negotiable invariants

1. **Little-endian only.** v1 does not attempt cross-endian mapping. Unsupported endianness is rejected.
2. **Explicit byte encoding.** No `repr(C)` struct is written directly to disk and no Rust struct is blindly memory-mapped as the persistence contract.
3. **The header is trusted first, and nothing else is trusted before it validates.** Magic, sizes, format compatibility, and header CRC must pass before the arena table is interpreted.
4. **The arena table is trusted second.** Its exact expected length and CRC must pass before any arena body is trusted or mapped.
5. **Each arena has an independent schema version.** Relations may evolve without forcing Entities, Episodes, Goals, or indexes to change format.
6. **Same-major older formats are readable only through code that explicitly understands them.** A newer runtime may project an older arena into the current API without mutating the file.
7. **Older major file formats require an explicit offline migration.** The normal open path does not rewrite a live cognitive image.
8. **Future formats are rejected.** An older runtime never guesses how to interpret newer metadata or arena layouts.
9. **Reserved bytes stay reserved until a format revision assigns them meaning.** Readers ignore them; writers currently emit zeroes.
10. **Generation is monotonic.** Generation counters are reserved for the crash-safe commit protocol; a later slice will define the exact atomic update sequence.

## File metadata v1

The file begins with a fixed 128-byte header. The header contains:

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

Initial arena IDs are:

1. Entity
2. Relation
3. Episode
4. Goal
5. Belief
6. Activation
7. TemporalIndex
8. Provenance
9. StringPool
10. FreeList

These IDs are persistent file-format identifiers. Renaming a Rust enum variant does not change its numeric arena ID.

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

## Why two integrity checks?

The header CRC answers: **can we trust the metadata that tells us where everything is?**

The arena-table CRC answers: **can we trust the descriptors that tell us how the cognitive arenas are laid out?**

Arena body integrity and crash-safe generation commits will be specified separately; this first slice intentionally does not pretend CRC32 makes live persistence atomic.

## What v1 intentionally does not decide

This metadata layer does **not** yet freeze:

- Entity record layout
- Relation record layout
- variable-width payload encoding
- arena allocation/free-list mechanics
- mmap write protocol
- crash-safe superblock commit sequence
- compaction
- body checksums
- concurrency model

Those are subsequent design slices. The point of this layer is to make those future choices evolvable without invalidating Frank's entire persistent identity.
