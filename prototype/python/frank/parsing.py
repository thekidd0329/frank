from __future__ import annotations

import json
import math
from datetime import datetime
from pathlib import Path
from typing import Any
from uuid import uuid4

from .models import Evidence


SOURCE_RELIABILITY = {
    "messages": 0.95,
    "user_history": 0.90,
    "profile_current": 0.85,
    "posts": 0.80,
    "profile_old": 0.60,
    "unknown": 0.50,
}


def parse_timestamp(value: str | None, fallback: datetime | None = None) -> datetime:
    if not value:
        return fallback or datetime.now()
    normalized = value.replace("Z", "+00:00")
    try:
        return datetime.fromisoformat(normalized)
    except ValueError:
        return fallback or datetime.now()


def freshness_from_timestamp(
    timestamp: datetime,
    now: datetime | None = None,
    half_life_days: float = 14.0,
) -> float:
    """Per-item exponential freshness with a literal half-life."""
    now = now or datetime.now(tz=timestamp.tzinfo) if timestamp.tzinfo else datetime.now()
    age_days = max(0.0, (now - timestamp).total_seconds() / 86400.0)
    return math.exp(-math.log(2.0) * age_days / half_life_days)


def classify_source(path: str | Path, payload: Any | None = None) -> str:
    p = Path(path)
    lower = str(p).lower()
    if "messages" in lower:
        return "messages"
    if "history" in lower:
        return "user_history"
    if "posts" in lower:
        return "posts"
    if "profile" in lower or (
        isinstance(payload, dict)
        and any(k in payload for k in ("favorite_style", "favorite_aesthetic", "bio", "status"))
        and "drawing_history" not in payload
    ):
        status = ""
        if isinstance(payload, dict):
            status = str(payload.get("status", "")).lower()
        if "old" in lower or "old" in status:
            return "profile_old"
        return "profile_current"
    return "unknown"


def _make_evidence(
    *,
    path: Path,
    content: str,
    claim: str,
    timestamp: datetime,
    reliability: float,
    supports: list[str] | None = None,
    contradicts: list[str] | None = None,
    axis: str | None = None,
    value: str | None = None,
    polarity: int | None = None,
    identity_score: float = 1.0,
    metadata: dict[str, Any] | None = None,
) -> Evidence:
    return Evidence(
        id=str(uuid4()),
        claim=claim,
        source=str(path),
        reliability=reliability,
        freshness=freshness_from_timestamp(timestamp),
        supports=supports or [],
        contradicts=contradicts or [],
        metadata=metadata or {},
        content=content,
        timestamp=timestamp,
        identity_score=identity_score,
        axis=axis,
        value=value,
        polarity=polarity,
    )


def _parse_profile(path: Path, data: dict[str, Any], base_rel: float) -> list[Evidence]:
    ts = parse_timestamp(data.get("last_updated"), datetime.fromtimestamp(path.stat().st_mtime))
    out: list[Evidence] = []

    aesthetic = data.get("favorite_style") or data.get("favorite_aesthetic")
    if aesthetic:
        normalized = str(aesthetic).lower()
        value = "minimalism" if "minimal" in normalized else normalized
        out.append(_make_evidence(
            path=path,
            content=str(aesthetic),
            claim=f"Lexie profile preference: {aesthetic}",
            timestamp=ts,
            reliability=base_rel,
            supports=["lexie_likes_minimalism"] if value == "minimalism" else [],
            axis="aesthetic_preference",
            value=value,
            polarity=1,
            metadata={"record_type": "profile_preference", "status": data.get("status")},
        ))

    bio = str(data.get("bio", ""))
    if bio:
        low = bio.lower()
        if "moon" in low:
            out.append(_make_evidence(
                path=path,
                content=bio,
                claim="Lexie's current bio contains a recurring moon motif",
                timestamp=ts,
                reliability=base_rel,
                supports=["moon_is_recurring_motif"],
                axis="visual_motif",
                value="moon",
                polarity=1,
                metadata={"record_type": "profile_bio", "status": data.get("status")},
            ))

    if data.get("name"):
        out.append(_make_evidence(
            path=path,
            content=json.dumps({"name": data.get("name"), "email": data.get("email"), "status": data.get("status")}),
            claim=f"Profile identity candidate: {data.get('name')}",
            timestamp=ts,
            reliability=base_rel,
            supports=["lexie_current_identity"] if "current" in str(data.get("status", "")).lower() else [],
            axis="identity",
            value=str(data.get("email") or data.get("name")),
            polarity=1,
            identity_score=0.98 if "current" in str(data.get("status", "")).lower() else 0.65,
            metadata={"record_type": "identity", "status": data.get("status")},
        ))
    return out


def _parse_text_record(path: Path, item: dict[str, Any], base_rel: float) -> list[Evidence]:
    text = str(item.get("text") or item.get("content") or item.get("caption") or "").strip()
    if not text:
        return []
    ts = parse_timestamp(item.get("timestamp") or item.get("date"), datetime.fromtimestamp(path.stat().st_mtime))
    low = text.lower()
    supports: list[str] = []
    contradicts: list[str] = []
    axis: str | None = None
    value: str | None = None
    polarity: int | None = None

    if "minimal" in low:
        axis = "aesthetic_preference"
        value = "minimalism"
        negative = (
            "unfinished" in low
            or "boring" in low
            or "hate" in low
            or "dislike" in low
            or "🙄" in text
            or str(item.get("tone", "")).lower() == "sarcastic"
        )
        polarity = -1 if negative else 1
        (contradicts if negative else supports).append("lexie_likes_minimalism")

    if "moon" in low:
        supports.append("moon_is_recurring_motif")
        if axis is None:
            axis = "visual_motif"
            value = "moon"
            polarity = 1

    return [_make_evidence(
        path=path,
        content=text,
        claim=text,
        timestamp=ts,
        reliability=base_rel,
        supports=supports,
        contradicts=contradicts,
        axis=axis,
        value=value,
        polarity=polarity,
        metadata={
            "record_type": "message_or_post",
            "author": item.get("sender") or item.get("from") or item.get("author"),
            "tone": item.get("tone"),
        },
    )]


def _parse_history(path: Path, data: dict[str, Any], base_rel: float) -> list[Evidence]:
    out: list[Evidence] = []
    for record in data.get("drawing_history", []):
        concept = str(record.get("concept", ""))
        note = str(record.get("note", ""))
        ts = parse_timestamp(record.get("timestamp") or record.get("date"), datetime.fromtimestamp(path.stat().st_mtime))
        contradicts = ["botanical_is_novel"] if "botanical" in concept.lower() or "botanical" in note.lower() else []
        out.append(_make_evidence(
            path=path,
            content=note or concept,
            claim=note or f"Prior drawing: {concept}",
            timestamp=ts,
            reliability=base_rel,
            contradicts=contradicts,
            axis="drawing_history",
            value=concept.lower() or None,
            polarity=1,
            metadata={"record_type": "drawing_history", "concept": concept},
        ))
    return out


def parse_file(path: str | Path) -> list[Evidence]:
    """Turn a supported local JSON/text file into structured Evidence objects."""
    path = Path(path)
    if not path.exists() or not path.is_file():
        return []

    try:
        if path.suffix.lower() == ".json":
            data = json.loads(path.read_text(encoding="utf-8"))
            source_type = classify_source(path, data)
            base_rel = SOURCE_RELIABILITY[source_type]
            if isinstance(data, dict) and source_type.startswith("profile"):
                return _parse_profile(path, data, base_rel)
            if isinstance(data, dict) and source_type == "user_history":
                return _parse_history(path, data, base_rel)
            if isinstance(data, list):
                out: list[Evidence] = []
                for item in data:
                    if isinstance(item, dict):
                        out.extend(_parse_text_record(path, item, base_rel))
                return out
            return []

        if path.suffix.lower() in {".txt", ".md"}:
            text = path.read_text(encoding="utf-8", errors="ignore")
            ts = datetime.fromtimestamp(path.stat().st_mtime)
            base_rel = SOURCE_RELIABILITY[classify_source(path)] * 0.7
            return [_make_evidence(
                path=path,
                content=text[:2000],
                claim=text[:500],
                timestamp=ts,
                reliability=base_rel,
                metadata={"record_type": "plain_text"},
            )]
    except (OSError, ValueError, TypeError, json.JSONDecodeError):
        return []

    return []


def is_semantic_opposite(a: Evidence, b: Evidence) -> bool:
    return (
        a.axis is not None
        and a.axis == b.axis
        and a.value is not None
        and a.value == b.value
        and a.polarity is not None
        and b.polarity is not None
        and a.polarity * b.polarity < 0
    )
