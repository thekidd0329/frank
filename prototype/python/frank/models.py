from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any
from datetime import datetime


class SideEffect(str, Enum):
    NONE = "none"
    EXTERNAL = "external"


@dataclass(slots=True)
class Goal:
    text: str
    success_criteria: list[str] = field(default_factory=list)


@dataclass(slots=True)
class Evidence:
    id: str
    claim: str
    source: str
    reliability: float
    freshness: float
    supports: list[str] = field(default_factory=list)
    contradicts: list[str] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)
    content: str | None = None
    timestamp: datetime | None = None
    identity_score: float = 1.0
    axis: str | None = None
    value: str | None = None
    polarity: int | None = None

    @property
    def weight(self) -> float:
        return max(0.0, min(1.0, self.reliability)) * max(0.0, min(1.0, self.freshness))


@dataclass(slots=True)
class Hypothesis:
    id: str
    claim: str
    evidence_ids: list[str] = field(default_factory=list)
    unresolved_questions: list[str] = field(default_factory=list)
    rejected: bool = False
    rejection_reason: str | None = None


@dataclass(slots=True)
class ActionCandidate:
    tool: str
    arguments: dict[str, Any]
    expected_information_gain: float
    side_effect: SideEffect = SideEffect.NONE
    rationale: str = ""


@dataclass(slots=True)
class Observation:
    action: ActionCandidate
    result: Any
    success: bool
    error: str | None = None


@dataclass(slots=True)
class ConfidenceFeatures:
    identity_certainty: float = 0.0
    source_agreement: float = 0.0
    evidence_freshness: float = 0.0
    evidence_quantity: float = 0.0
    contradiction_severity: float = 1.0
    inference_distance: float = 1.0
    missing_critical_info: bool = True


@dataclass(slots=True)
class CognitiveState:
    goal: Goal
    evidence: dict[str, Evidence] = field(default_factory=dict)
    hypotheses: dict[str, Hypothesis] = field(default_factory=dict)
    unresolved_questions: list[str] = field(default_factory=list)
    observations: list[Observation] = field(default_factory=list)
    confidence_features: ConfidenceFeatures = field(default_factory=ConfidenceFeatures)
    step: int = 0
    done: bool = False
    final_answer: str | None = None
    proposed_external_action: ActionCandidate | None = None
