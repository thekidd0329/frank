from __future__ import annotations

from pathlib import Path
from .calibration import provisional_confidence
from .models import ActionCandidate, CognitiveState, Goal
from .planner import AutonomousPlanner
from .parsing import parse_file
from .tools import SandboxSearchTool


QUERIES = [
    ("current profile", None, 0.95, "Identify the current Lexie before reasoning about preferences."),
    ("minimalist", None, 0.80, "Test whether minimalist art is actually a current preference."),
    ("botanical", None, 0.65, "Check whether botanical imagery has already been used."),
    ("moon", None, 0.55, "Investigate a recurring alternative motif."),
]


def build_state() -> CognitiveState:
    return CognitiveState(
        goal=Goal(
            text="Figure out what I should draw Lexie.",
            success_criteria=[
                "identify correct Lexie",
                "resolve stale/conflicting preferences",
                "avoid repeating prior drawing concepts",
                "recommend one defensible concept",
            ],
        ),
        unresolved_questions=[
            "Which Lexie is current?",
            "What aesthetic preferences are current?",
            "What has Christian already drawn for her?",
        ],
    )


def run_benchmark(root: str | Path) -> CognitiveState:
    tool = SandboxSearchTool(root)
    state = build_state()
    query_index = 0

    def propose_next(s: CognitiveState) -> ActionCandidate | None:
        nonlocal query_index
        if query_index >= len(QUERIES):
            return None
        query, scope, gain, why = QUERIES[query_index]
        query_index += 1
        return ActionCandidate(
            tool="sandbox.search",
            arguments={"query": query, "scope": scope},
            expected_information_gain=gain,
            rationale=why,
        )

    def execute(action: ActionCandidate):
        return tool.call(**action.arguments)

    def integrate(s: CognitiveState, obs):
        if not obs.success:
            return
        for result in obs.result:
            for ev in parse_file(Path(root) / result["path"]):
                fingerprint = (ev.source, ev.claim, ev.axis, ev.value, ev.polarity)
                if any(
                    (x.source, x.claim, x.axis, x.value, x.polarity) == fingerprint
                    for x in s.evidence.values()
                ):
                    continue
                s.evidence[ev.id] = ev

        all_ev = list(s.evidence.values())
        s.confidence_features.identity_certainty = max(
            [e.identity_score for e in all_ev if e.axis == "identity"],
            default=0.5,
        )
        s.confidence_features.evidence_freshness = sum(e.freshness for e in all_ev) / max(1, len(all_ev))
        s.confidence_features.evidence_quantity = min(1.0, len(all_ev) / 6.0)

        mini_support = sum(1 for e in all_ev if "lexie_likes_minimalism" in e.supports)
        mini_contra = sum(1 for e in all_ev if "lexie_likes_minimalism" in e.contradicts)
        s.confidence_features.source_agreement = 1.0 if mini_support == 0 or mini_contra == 0 else 0.45
        s.confidence_features.contradiction_severity = 0.8 if mini_support and mini_contra else 0.1
        s.confidence_features.inference_distance = 0.35
        s.confidence_features.missing_critical_info = not any(
            "moon_is_recurring_motif" in e.supports for e in all_ev
        )

    def should_stop(s: CognitiveState) -> bool:
        all_ev = list(s.evidence.values())
        has_minimalism_conflict = (
            any("lexie_likes_minimalism" in e.supports for e in all_ev)
            and any("lexie_likes_minimalism" in e.contradicts for e in all_ev)
        )
        has_repeat_warning = any("botanical_is_novel" in e.contradicts for e in all_ev)
        has_alt_motif = any("moon_is_recurring_motif" in e.supports for e in all_ev)

        if has_minimalism_conflict and has_repeat_warning and has_alt_motif:
            score = provisional_confidence(s.confidence_features)
            s.final_answer = (
                "Recommend a detailed moon-centered portrait/concept rather than minimalist or botanical art. "
                "Recent direct evidence contradicts the stale minimalist preference, and botanical work would repeat "
                f"a prior drawing. Provisional (not yet calibrated) confidence: {score:.2f}."
            )
            return True
        return False

    planner = AutonomousPlanner(propose_next, execute, integrate, should_stop)
    return planner.run(state)
