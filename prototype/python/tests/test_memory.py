from frank.combing import CombingEngine, LearnedAssociation
from frank.memory import CompactClaim, Ontology, PersonMemory


def test_ontology_self_expands_and_ids_stay_stable():
    ontology = Ontology()

    axis = ontology.get_or_create_axis("aesthetic preference")
    gothic = ontology.get_or_create_value(axis, "gothic")
    same_axis = ontology.get_or_create_axis("Aesthetic-Preference")
    same_gothic = ontology.get_or_create_value(axis, "Gothic")

    assert axis == same_axis
    assert gothic == same_gothic

    brutalist = ontology.get_or_create_value(axis, "brutalist")
    assert brutalist != gothic
    assert ontology.resolve_value(axis, gothic) == "gothic"


def test_compact_claim_is_four_numbers_and_six_bytes():
    claim = CompactClaim(1, 7, 94, 1)

    assert claim.as_numbers() == (1, 7, 94, 1)
    payload = claim.pack()
    assert len(payload) == 6
    assert CompactClaim.unpack(payload) == claim


def test_combing_persists_association_without_source():
    ontology = Ontology()
    memory = PersonMemory("lexie")
    engine = CombingEngine(ontology)

    claim = engine.learn(
        LearnedAssociation(
            person_id="lexie",
            axis="aesthetic preference",
            value="gothic",
            confidence=94,
            recency=1,
        ),
        memory,
    )

    assert memory.get(claim.axis_id, claim.value_id) == claim
    assert not hasattr(claim, "source")
    assert not hasattr(claim, "content")


def test_combing_can_create_new_axis_and_value():
    ontology = Ontology()
    memory = PersonMemory("lexie")
    engine = CombingEngine(ontology)

    claim = engine.learn(
        LearnedAssociation(
            person_id="lexie",
            axis="social boundary",
            value="public surprises",
            confidence=91,
            recency=2,
        ),
        memory,
    )

    assert ontology.resolve_axis(claim.axis_id) == "social_boundary"
    assert ontology.resolve_value(claim.axis_id, claim.value_id) == "public_surprises"
