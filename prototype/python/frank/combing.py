from __future__ import annotations

from dataclasses import dataclass

from .memory import CompactClaim, Ontology, PersonMemory


@dataclass(frozen=True, slots=True)
class LearnedAssociation:
    """
    Ephemeral handoff from combing/parser intelligence into long-term memory.

    The source may exist upstream while reasoning, but is intentionally absent
    here so persistent person memory never stores source material.
    """

    person_id: str
    axis: str
    value: str
    confidence: int
    recency: int


class CombingEngine:
    def __init__(self, ontology: Ontology):
        self.ontology = ontology

    def learn(
        self,
        association: LearnedAssociation,
        person_memory: PersonMemory,
    ) -> CompactClaim:
        if association.person_id != person_memory.person_id:
            raise ValueError("association person does not match target person memory")

        axis_id = self.ontology.get_or_create_axis(association.axis)
        value_id = self.ontology.get_or_create_value(axis_id, association.value)
        claim = CompactClaim(
            axis_id=axis_id,
            value_id=value_id,
            confidence=association.confidence,
            recency=association.recency,
        )
        return person_memory.upsert(claim)
