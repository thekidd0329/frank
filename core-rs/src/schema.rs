//! Stable on-disk metadata for Frank's packed cognitive image.
//!
//! Design rule: the file format is NEVER the in-memory Rust struct layout.
//! Every field is encoded/decoded explicitly as little-endian bytes.
//! This keeps `frank.cog` stable across compiler, platform, and refactors.
//!
//! Ground-state rule: `ArenaId::ResidualCommitment` is the authoritative
//! cognitive arena. Symbolic arenas retained from the earlier schema are
//! compatibility/projection caches, not independent cognitive truth.

use core::fmt;

pub const MAGIC: [u8; 8] = *b"FRANKCOG";
pub const ENDIAN_LITTLE: u8 = 1;
pub const HEADER_BYTES: usize = 128;
pub const ARENA_DESCRIPTOR_BYTES: usize = 64;
pub const DEFAULT_ARENA_TABLE_OFFSET: u64 = 4096;

pub const FORMAT_MAJOR: u16 = 1;
pub const FORMAT_MINOR: u16 = 0;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct FormatVersion {
    pub major: u16,
    pub minor: u16,
}

impl FormatVersion {
    pub const CURRENT: Self = Self {
        major: FORMAT_MAJOR,
        minor: FORMAT_MINOR,
    };
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Compatibility {
    Readable,
    OfflineMigrationRequired {
        file: FormatVersion,
        runtime: FormatVersion,
    },
    UnsupportedFuture {
        file: FormatVersion,
        runtime: FormatVersion,
    },
}

pub const fn compatibility(file: FormatVersion, runtime: FormatVersion) -> Compatibility {
    if file.major < runtime.major {
        Compatibility::OfflineMigrationRequired { file, runtime }
    } else if file.major > runtime.major || file.minor > runtime.minor {
        Compatibility::UnsupportedFuture { file, runtime }
    } else {
        Compatibility::Readable
    }
}

/// Persistent arena identifiers.
///
/// IDs 1..=10 predate the Residual Commitment inversion and are intentionally
/// retained to avoid gratuitous format churn on the experimental branch.
/// They are projection/cache/support arenas unless explicitly documented
/// otherwise. ID 11 is the authoritative ground cognitive arena.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
#[repr(u16)]
pub enum ArenaId {
    Entity = 1,
    Relation = 2,
    Episode = 3,
    Goal = 4,
    Belief = 5,
    Activation = 6,
    TemporalIndex = 7,
    Provenance = 8,
    StringPool = 9,
    FreeList = 10,
    ResidualCommitment = 11,
}

impl ArenaId {
    pub const fn from_raw(raw: u16) -> Option<Self> {
        match raw {
            1 => Some(Self::Entity),
            2 => Some(Self::Relation),
            3 => Some(Self::Episode),
            4 => Some(Self::Goal),
            5 => Some(Self::Belief),
            6 => Some(Self::Activation),
            7 => Some(Self::TemporalIndex),
            8 => Some(Self::Provenance),
            9 => Some(Self::StringPool),
            10 => Some(Self::FreeList),
            11 => Some(Self::ResidualCommitment),
            _ => None,
        }
    }

    /// Whether this arena is the source of persistent cognitive truth.
    pub const fn is_cognitive_ground(self) -> bool {
        matches!(self, Self::ResidualCommitment)
    }
}

pub mod arena_flags {
    pub const SPARSE: u32 = 1 << 0;
    pub const SORTED: u32 = 1 << 1;
    pub const HAS_FREE_LIST: u32 = 1 << 2;
    pub const VARIABLE_WIDTH: u32 = 1 << 3;
    pub const READ_ONLY: u32 = 1 << 4;
    /// Marks an arena as a rebuildable projection/cache rather than ground truth.
    pub const REBUILDABLE_CACHE: u32 = 1 << 5;
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct FileHeader {
    pub version: FormatVersion,
    pub flags: u16,
    pub created_at_ms: u64,
    pub last_written_ms: u64,
    /// Monotonically increasing successful commit generation.
    pub generation: u64,
    pub arena_table_offset: u64,
    pub arena_count: u16,
    pub image_len: u64,
    /// CRC32 over the encoded arena descriptor table.
    pub arena_table_crc32: u32,
}

impl FileHeader {
    pub const fn new(created_at_ms: u64) -> Self {
        Self {
            version: FormatVersion::CURRENT,
            flags: 0,
            created_at_ms,
            last_written_ms: created_at_ms,
            generation: 0,
            arena_table_offset: DEFAULT_ARENA_TABLE_OFFSET,
            arena_count: 0,
            image_len: DEFAULT_ARENA_TABLE_OFFSET,
            arena_table_crc32: 0,
        }
    }

    pub fn encode(self) -> [u8; HEADER_BYTES] {
        let mut out = [0u8; HEADER_BYTES];
        out[0..8].copy_from_slice(&MAGIC);
        put_u16(&mut out, 8, self.version.major);
        put_u16(&mut out, 10, self.version.minor);
        out[12] = ENDIAN_LITTLE;
        out[13] = 0;
        put_u16(&mut out, 14, self.flags);
        put_u16(&mut out, 16, HEADER_BYTES as u16);
        put_u16(&mut out, 18, ARENA_DESCRIPTOR_BYTES as u16);
        put_u16(&mut out, 20, self.arena_count);
        put_u16(&mut out, 22, 0);
        put_u64(&mut out, 24, self.created_at_ms);
        put_u64(&mut out, 32, self.last_written_ms);
        put_u64(&mut out, 40, self.generation);
        put_u64(&mut out, 48, self.arena_table_offset);
        put_u64(&mut out, 56, self.image_len);
        put_u32(&mut out, 64, self.arena_table_crc32);
        let header_crc = crc32(&out[..124]);
        put_u32(&mut out, 124, header_crc);
        out
    }

    pub fn decode(bytes: &[u8]) -> Result<Self, SchemaError> {
        if bytes.len() < HEADER_BYTES {
            return Err(SchemaError::TruncatedHeader {
                actual: bytes.len(),
            });
        }
        if bytes[0..8] != MAGIC {
            return Err(SchemaError::BadMagic);
        }
        if bytes[12] != ENDIAN_LITTLE {
            return Err(SchemaError::UnsupportedEndianness(bytes[12]));
        }
        if get_u16(bytes, 16) as usize != HEADER_BYTES {
            return Err(SchemaError::UnexpectedHeaderSize(get_u16(bytes, 16)));
        }
        if get_u16(bytes, 18) as usize != ARENA_DESCRIPTOR_BYTES {
            return Err(SchemaError::UnexpectedDescriptorSize(get_u16(bytes, 18)));
        }

        let expected_crc = get_u32(bytes, 124);
        let actual_crc = crc32(&bytes[..124]);
        if expected_crc != actual_crc {
            return Err(SchemaError::HeaderChecksumMismatch {
                expected: expected_crc,
                actual: actual_crc,
            });
        }

        let version = FormatVersion {
            major: get_u16(bytes, 8),
            minor: get_u16(bytes, 10),
        };

        match compatibility(version, FormatVersion::CURRENT) {
            Compatibility::Readable => {}
            Compatibility::OfflineMigrationRequired { file, runtime } => {
                return Err(SchemaError::OfflineMigrationRequired { file, runtime });
            }
            Compatibility::UnsupportedFuture { file, runtime } => {
                return Err(SchemaError::UnsupportedFuture { file, runtime });
            }
        }

        Ok(Self {
            version,
            flags: get_u16(bytes, 14),
            arena_count: get_u16(bytes, 20),
            created_at_ms: get_u64(bytes, 24),
            last_written_ms: get_u64(bytes, 32),
            generation: get_u64(bytes, 40),
            arena_table_offset: get_u64(bytes, 48),
            image_len: get_u64(bytes, 56),
            arena_table_crc32: get_u32(bytes, 64),
        })
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ArenaDescriptor {
    pub arena_id: ArenaId,
    pub arena_version: u16,
    pub flags: u32,
    /// Fixed record width. Must be zero when VARIABLE_WIDTH is set.
    pub element_size: u32,
    pub capacity: u64,
    pub count: u64,
    pub offset: u64,
    pub byte_len: u64,
    pub generation: u64,
}

impl ArenaDescriptor {
    pub fn encode(self) -> [u8; ARENA_DESCRIPTOR_BYTES] {
        let mut out = [0u8; ARENA_DESCRIPTOR_BYTES];
        put_u16(&mut out, 0, self.arena_id as u16);
        put_u16(&mut out, 2, self.arena_version);
        put_u32(&mut out, 4, self.flags);
        put_u32(&mut out, 8, self.element_size);
        put_u32(&mut out, 12, 0);
        put_u64(&mut out, 16, self.capacity);
        put_u64(&mut out, 24, self.count);
        put_u64(&mut out, 32, self.offset);
        put_u64(&mut out, 40, self.byte_len);
        put_u64(&mut out, 48, self.generation);
        put_u64(&mut out, 56, 0);
        out
    }

    pub fn decode(bytes: &[u8]) -> Result<Self, SchemaError> {
        if bytes.len() < ARENA_DESCRIPTOR_BYTES {
            return Err(SchemaError::TruncatedArenaDescriptor {
                actual: bytes.len(),
            });
        }
        let raw_id = get_u16(bytes, 0);
        let arena_id = ArenaId::from_raw(raw_id).ok_or(SchemaError::UnknownArenaId(raw_id))?;
        let descriptor = Self {
            arena_id,
            arena_version: get_u16(bytes, 2),
            flags: get_u32(bytes, 4),
            element_size: get_u32(bytes, 8),
            capacity: get_u64(bytes, 16),
            count: get_u64(bytes, 24),
            offset: get_u64(bytes, 32),
            byte_len: get_u64(bytes, 40),
            generation: get_u64(bytes, 48),
        };
        descriptor.validate()?;
        Ok(descriptor)
    }

    pub fn validate(self) -> Result<(), SchemaError> {
        if self.count > self.capacity {
            return Err(SchemaError::CountExceedsCapacity {
                count: self.count,
                capacity: self.capacity,
            });
        }
        let variable = self.flags & arena_flags::VARIABLE_WIDTH != 0;
        if variable && self.element_size != 0 {
            return Err(SchemaError::VariableWidthHasElementSize(self.element_size));
        }
        if !variable && self.capacity != 0 && self.element_size == 0 {
            return Err(SchemaError::FixedWidthMissingElementSize);
        }
        Ok(())
    }
}

pub fn verify_arena_table(header: &FileHeader, table: &[u8]) -> Result<(), SchemaError> {
    let expected_len = header.arena_count as usize * ARENA_DESCRIPTOR_BYTES;
    if table.len() != expected_len {
        return Err(SchemaError::ArenaTableLengthMismatch {
            expected: expected_len,
            actual: table.len(),
        });
    }
    let actual = crc32(table);
    if actual != header.arena_table_crc32 {
        return Err(SchemaError::ArenaTableChecksumMismatch {
            expected: header.arena_table_crc32,
            actual,
        });
    }
    Ok(())
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ArenaCompatibility {
    Exact,
    ReadOlder { file_version: u16, current_version: u16 },
    UnsupportedFuture { file_version: u16, current_version: u16 },
}

pub const fn arena_compatibility(file_version: u16, current_version: u16) -> ArenaCompatibility {
    if file_version == current_version {
        ArenaCompatibility::Exact
    } else if file_version < current_version {
        ArenaCompatibility::ReadOlder {
            file_version,
            current_version,
        }
    } else {
        ArenaCompatibility::UnsupportedFuture {
            file_version,
            current_version,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum SchemaError {
    TruncatedHeader { actual: usize },
    TruncatedArenaDescriptor { actual: usize },
    BadMagic,
    UnsupportedEndianness(u8),
    UnexpectedHeaderSize(u16),
    UnexpectedDescriptorSize(u16),
    HeaderChecksumMismatch { expected: u32, actual: u32 },
    ArenaTableLengthMismatch { expected: usize, actual: usize },
    ArenaTableChecksumMismatch { expected: u32, actual: u32 },
    UnknownArenaId(u16),
    CountExceedsCapacity { count: u64, capacity: u64 },
    VariableWidthHasElementSize(u32),
    FixedWidthMissingElementSize,
    OfflineMigrationRequired { file: FormatVersion, runtime: FormatVersion },
    UnsupportedFuture { file: FormatVersion, runtime: FormatVersion },
}

impl fmt::Display for SchemaError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{self:?}")
    }
}

impl std::error::Error for SchemaError {}

pub fn descriptor_table_crc32(descriptors: &[ArenaDescriptor]) -> u32 {
    let mut crc = !0u32;
    for descriptor in descriptors {
        for byte in descriptor.encode() {
            crc = crc32_update(crc, byte);
        }
    }
    !crc
}

pub fn crc32(bytes: &[u8]) -> u32 {
    let mut crc = !0u32;
    for &byte in bytes {
        crc = crc32_update(crc, byte);
    }
    !crc
}

fn crc32_update(mut crc: u32, byte: u8) -> u32 {
    crc ^= byte as u32;
    let mut bit = 0;
    while bit < 8 {
        let mask = (crc & 1).wrapping_neg();
        crc = (crc >> 1) ^ (0xEDB8_8320u32 & mask);
        bit += 1;
    }
    crc
}

fn put_u16(out: &mut [u8], at: usize, value: u16) {
    out[at..at + 2].copy_from_slice(&value.to_le_bytes());
}

fn put_u32(out: &mut [u8], at: usize, value: u32) {
    out[at..at + 4].copy_from_slice(&value.to_le_bytes());
}

fn put_u64(out: &mut [u8], at: usize, value: u64) {
    out[at..at + 8].copy_from_slice(&value.to_le_bytes());
}

fn get_u16(bytes: &[u8], at: usize) -> u16 {
    u16::from_le_bytes([bytes[at], bytes[at + 1]])
}

fn get_u32(bytes: &[u8], at: usize) -> u32 {
    u32::from_le_bytes([
        bytes[at],
        bytes[at + 1],
        bytes[at + 2],
        bytes[at + 3],
    ])
}

fn get_u64(bytes: &[u8], at: usize) -> u64 {
    u64::from_le_bytes([
        bytes[at],
        bytes[at + 1],
        bytes[at + 2],
        bytes[at + 3],
        bytes[at + 4],
        bytes[at + 5],
        bytes[at + 6],
        bytes[at + 7],
    ])
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn header_round_trip_is_stable() {
        let mut header = FileHeader::new(1234);
        header.last_written_ms = 5678;
        header.generation = 42;
        header.arena_count = 2;
        header.image_len = 9999;
        header.arena_table_crc32 = 0x12345678;
        let bytes = header.encode();
        assert_eq!(FileHeader::decode(&bytes).unwrap(), header);
    }

    #[test]
    fn corrupted_header_is_rejected() {
        let header = FileHeader::new(1234);
        let mut bytes = header.encode();
        bytes[50] ^= 0xFF;
        assert!(matches!(
            FileHeader::decode(&bytes),
            Err(SchemaError::HeaderChecksumMismatch { .. })
        ));
    }

    #[test]
    fn residual_commitment_arena_is_ground_and_round_trips() {
        let descriptor = ArenaDescriptor {
            arena_id: ArenaId::ResidualCommitment,
            arena_version: 1,
            flags: arena_flags::SPARSE | arena_flags::SORTED | arena_flags::HAS_FREE_LIST,
            element_size: 16,
            capacity: 1_000_000,
            count: 42,
            offset: 8192,
            byte_len: 16_000_000,
            generation: 7,
        };
        assert!(descriptor.arena_id.is_cognitive_ground());
        let bytes = descriptor.encode();
        assert_eq!(ArenaDescriptor::decode(&bytes).unwrap(), descriptor);
    }

    #[test]
    fn legacy_symbolic_arena_is_not_ground() {
        assert!(!ArenaId::Belief.is_cognitive_ground());
        assert!(!ArenaId::Relation.is_cognitive_ground());
    }

    #[test]
    fn older_major_requires_offline_migration() {
        let file = FormatVersion { major: 0, minor: 9 };
        assert!(matches!(
            compatibility(file, FormatVersion::CURRENT),
            Compatibility::OfflineMigrationRequired { .. }
        ));
    }

    #[test]
    fn newer_minor_is_refused() {
        let file = FormatVersion {
            major: FORMAT_MAJOR,
            minor: FORMAT_MINOR + 1,
        };
        assert!(matches!(
            compatibility(file, FormatVersion::CURRENT),
            Compatibility::UnsupportedFuture { .. }
        ));
    }

    #[test]
    fn arena_table_crc_detects_damage() {
        let descriptors = [ArenaDescriptor {
            arena_id: ArenaId::ResidualCommitment,
            arena_version: 1,
            flags: arena_flags::SPARSE,
            element_size: 16,
            capacity: 8,
            count: 2,
            offset: 8192,
            byte_len: 128,
            generation: 1,
        }];
        let mut table = descriptors[0].encode().to_vec();
        let mut header = FileHeader::new(0);
        header.arena_count = 1;
        header.arena_table_crc32 = descriptor_table_crc32(&descriptors);
        verify_arena_table(&header, &table).unwrap();
        table[11] ^= 1;
        assert!(matches!(
            verify_arena_table(&header, &table),
            Err(SchemaError::ArenaTableChecksumMismatch { .. })
        ));
    }
}
