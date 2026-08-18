from __future__ import annotations

from dataclasses import dataclass, field
import re
import struct

RECENCY_BUCKETS = {
    0: "now",
    1: "under_3_days",
    2: "under_2_weeks",
    3: "under_2_months",
    4: "under_6_months",
    5: "under_1_year",
    6: "old",
    7: "unknown",
}

_CLAIM_STRUCT = struct.Struct(">HHBB")  # axis, value, confidence, recency


def _canonical_token(value: str) -> str:
    token = value.strip().lower()
    token = re.sub(r"[\s\-]+", "_", token)
    token = re.sub(r"[^a-z0-9_]+", "", token)
    return token


@dataclass(frozen=True, slots=True)
class CompactClaim:
    """Persistent atomic memory: [axis, value, confidence, recency]."""

    axis_id: int
    value_id: int
    confidence: int
    recency: int

    def __post_init__(self) -> None:
        if not 1 <= self.axis_id <= 0xFFFF:
            raise ValueError("axis_id must be between 1 and 65535")
        if not 1 <= self.value_id <= 0xFFFF:
            raise ValueError("value_id must be between 1 and 65535")
        if not 0 <= self.confidence <= 100:
            raise ValueError("confidence must be between 0 and 100")
        if self.recency not in RECENCY_BUCKETS:
            raise ValueError("recency must be a defined bucket")

    def as_numbers(self) -> tuple[int, int, int, int]:
        return (self.axis_id, self.value_id, self.confidence, self.recency)

    def pack(self) -> bytes:
        return _CLAIM_STRUCT.pack(*self.as_numbers())

    @classmethod
    def unpack(cls, payload: bytes) -> "CompactClaim":
        if len(payload) != _CLAIM_STRUCT.size:
            raise ValueError(f"compact claim payload must be {_CLAIM_STRUCT.size} bytes")
        return cls(*_CLAIM_STRUCT.unpack(payload))


@dataclass(slots=True)
class Ontology:
    """
    Shared self-expanding numeric vocabulary.

    IDs are stable once assigned and are never repurposed. Meanings are stored
    once globally; person records only retain numeric claims.
    """

    axis_names: dict[int, str] = field(default_factory=dict)
    axis_ids: dict[str, int] = field(default_factory=dict)
    value_names: dict[int, dict[int, str]] = field(default_factory=dict)
    value_ids: dict[int, dict[str, int]] = field(default_factory=dict)

    def get_or_create_axis(self, concept: str) -> int:
        token = _canonical_token(concept)
        if not token:
            raise ValueError("axis concept cannot be empty")
        existing = self.axis_ids.get(token)
        if existing is not None:
            return existing

        new_id = max(self.axis_names, default=0) + 1
        if new_id > 0xFFFF:
            raise OverflowError("axis ontology exhausted 16-bit schema")
        self.axis_ids[token] = new_id
        self.axis_names[new_id] = token
        self.value_names.setdefault(new_id, {})
        self.value_ids.setdefault(new_id, {})
        return new_id

    def get_or_create_value(self, axis_id: int, concept: str) -> int:
        if axis_id not in self.axis_names:
            raise KeyError(f"unknown axis_id: {axis_id}")

        token = _canonical_token(concept)
        if not token:
            raise ValueError("value concept cannot be empty")

        axis_values = self.value_ids.setdefault(axis_id, {})
        existing = axis_values.get(token)
        if existing is not None:
            return existing

        names = self.value_names.setdefault(axis_id, {})
        new_id = max(names, default=0) + 1
        if new_id > 0xFFFF:
            raise OverflowError("value ontology exhausted 16-bit schema")
        axis_values[token] = new_id
        names[new_id] = token
        return new_id

    def resolve_axis(self, axis_id: int) -> str:
        return self.axis_names[axis_id]

    def resolve_value(self, axis_id: int, value_id: int) -> str:
        return self.value_names[axis_id][value_id]


@dataclass(slots=True)
class PersonMemory:
    person_id: str
    claims: dict[tuple[int, int], CompactClaim] = field(default_factory=dict)

    def upsert(self, claim: CompactClaim) -> CompactClaim:
        """
        Store only the latest compact state for an axis/value association.

        No source path, raw text, or evidence body is persisted here.
        """
        self.claims[(claim.axis_id, claim.value_id)] = claim
        return claim

    def get(self, axis_id: int, value_id: int) -> CompactClaim | None:
        return self.claims.get((axis_id, value_id))

    def packed(self) -> bytes:
        return b"".join(
            self.claims[key].pack()
            for key in sorted(self.claims)
        )
