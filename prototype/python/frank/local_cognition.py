from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, Set
import math
import os

from .models import Evidence
from .parsing import is_semantic_opposite, parse_file

HALF_LIFE_DAYS = 14.0
COLLISION_RELIABILITY = 0.70


@dataclass(slots=True)
class InvestigationHypothesis:
    id: str
    statement: str
    evidence: list[Evidence] = field(default_factory=list)
    contradictions: int = 0
    last_updated: datetime = field(default_factory=datetime.now)
    frozen: bool = False
    collision_axis: str | None = None
    collision_value: str | None = None

    def uncertainty(self, now: datetime | None = None) -> float:
        now = now or datetime.now()
        if not self.evidence:
            return 1.0

        decays: list[float] = []
        for e in self.evidence:
            if e.timestamp is None:
                decays.append(e.freshness)
                continue
            ref_now = now
            if e.timestamp.tzinfo is not None and ref_now.tzinfo is None:
                ref_now = datetime.now(tz=e.timestamp.tzinfo)
            age_days = max(0.0, (ref_now - e.timestamp).total_seconds() / 86400.0)
            decays.append(math.exp(-math.log(2.0) * age_days / HALF_LIFE_DAYS))

        freshness = sum(decays) / len(decays)
        qty = min(1.0, len(self.evidence) / 8.0)
        rel = sum(e.reliability for e in self.evidence) / len(self.evidence)
        dens = min(1.0, self.contradictions / max(1, len(self.evidence)))
        confidence = freshness * rel * qty * (1.0 - dens)
        return max(0.0, min(1.0, 1.0 - confidence))


@dataclass(slots=True)
class LocalActionCandidate:
    hypothesis_id: str
    action_type: str
    target_path: str
    expected_info_gain: float


@dataclass(slots=True)
class LocalCognitiveState:
    hypotheses: dict[str, InvestigationHypothesis] = field(default_factory=dict)
    cycle: int = 0
    consecutive_zero_eig: int = 0
    max_depth: int = 12
    eig_epsilon: float = 0.01
    n_zero_cycles: int = 3
    visited_sources: Set[str] = field(default_factory=set)


def select_next_hypothesis(state: LocalCognitiveState) -> Optional[InvestigationHypothesis]:
    """Frozen hypotheses remain selectable: freeze means 'seek tie-breaker', not 'stop learning'."""
    if not state.hypotheses:
        return None
    return max(
        state.hypotheses.values(),
        key=lambda h: h.uncertainty() + (0.15 if h.frozen else 0.0),
    )


def _same_semantic_axis(a: Evidence, b: Evidence) -> bool:
    return (
        a.axis is not None
        and a.axis == b.axis
        and a.value is not None
        and a.value == b.value
    )


def _high_reliability_votes(h: InvestigationHypothesis) -> tuple[float, float]:
    positive = 0.0
    negative = 0.0
    for e in h.evidence:
        if e.reliability < COLLISION_RELIABILITY or e.polarity not in (-1, 1):
            continue
        vote = e.reliability * max(0.05, e.freshness)
        if e.polarity > 0:
            positive += vote
        else:
            negative += vote
    return positive, negative


def resolve_collision(h: InvestigationHypothesis, new_ev: Evidence) -> bool:
    """
    Accept evidence even during a collision. A two-source high-reliability
    opposition freezes conclusions; a later independent tie-breaker can thaw it.
    """
    opposing = next(
        (
            e for e in h.evidence
            if min(e.reliability, new_ev.reliability) >= COLLISION_RELIABILITY
            and _same_semantic_axis(e, new_ev)
            and is_semantic_opposite(e, new_ev)
        ),
        None,
    )

    h.evidence.append(new_ev)
    h.last_updated = datetime.now()

    if opposing is not None and not h.frozen:
        h.frozen = True
        h.collision_axis = new_ev.axis
        h.collision_value = new_ev.value
        h.contradictions += 1
        return True

    if h.frozen:
        relevant = [
            e for e in h.evidence
            if e.axis == h.collision_axis
            and e.value == h.collision_value
            and e.reliability >= COLLISION_RELIABILITY
            and e.polarity in (-1, 1)
        ]
        if len({e.source for e in relevant}) >= 3:
            positive, negative = _high_reliability_votes(h)
            if abs(positive - negative) >= 0.15:
                h.frozen = False
                h.collision_axis = None
                h.collision_value = None

    return True


def expected_info_gain(h: InvestigationHypothesis, path: str, visited: Set[str]) -> float:
    if path in visited:
        return 0.0
    current_u = h.uncertainty()
    collision_bonus = 0.20 if h.frozen else 0.0
    return min(1.0, current_u * 0.4 + collision_bonus)


def execute_candidate(
    state: LocalCognitiveState,
    target: InvestigationHypothesis,
    candidate: LocalActionCandidate,
) -> int:
    evidence_items = parse_file(candidate.target_path)
    accepted = 0
    for ev in evidence_items:
        if ev.axis is None:
            continue
        if resolve_collision(target, ev):
            accepted += 1
    state.visited_sources.add(candidate.target_path)
    return accepted


def autonomous_loop(state: LocalCognitiveState, root: str = "/mock_phone") -> LocalCognitiveState:
    while state.cycle < state.max_depth:
        state.cycle += 1
        target = select_next_hypothesis(state)
        if target is None:
            break

        candidates: list[LocalActionCandidate] = []
        for dirpath, _, files in os.walk(root):
            for filename in files:
                path = os.path.join(dirpath, filename)
                eig = expected_info_gain(target, path, state.visited_sources)
                if eig > state.eig_epsilon:
                    candidates.append(LocalActionCandidate(
                        hypothesis_id=target.id,
                        action_type="read",
                        target_path=path,
                        expected_info_gain=eig,
                    ))

        if not candidates:
            state.consecutive_zero_eig += 1
            if state.consecutive_zero_eig >= state.n_zero_cycles:
                break
            continue

        best = max(candidates, key=lambda c: c.expected_info_gain)
        state.consecutive_zero_eig = 0
        execute_candidate(state, target, best)

    return state
