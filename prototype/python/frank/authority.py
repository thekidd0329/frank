from __future__ import annotations

from dataclasses import dataclass

from .models import ActionCandidate, SideEffect


@dataclass(slots=True)
class AuthorityDecision:
    allowed: bool
    confirmation_required: bool
    reason: str


class AuthorityFirewall:
    """
    Policy boundary outside cognition.

    Read-only/internal work is always allowed. External actions inside granted
    scope auto-execute at or above the calibrated confidence threshold; below it
    they require owner confirmation.
    """

    def __init__(self, auto_execute_threshold: float = 0.90):
        if not 0.0 <= auto_execute_threshold <= 1.0:
            raise ValueError("auto_execute_threshold must be between 0 and 1")
        self.auto_execute_threshold = auto_execute_threshold

    def evaluate(
        self,
        action: ActionCandidate,
        calibrated_confidence: float = 0.0,
        scope_permitted: bool = True,
    ) -> AuthorityDecision:
        if action.side_effect is SideEffect.NONE:
            return AuthorityDecision(True, False, "read-only/internal action")

        if not scope_permitted:
            return AuthorityDecision(False, False, "action is outside granted authority scope")

        confidence = max(0.0, min(1.0, calibrated_confidence))
        if confidence >= self.auto_execute_threshold:
            return AuthorityDecision(
                True,
                False,
                f"calibrated confidence {confidence:.2f} meets auto-execute threshold",
            )

        return AuthorityDecision(
            False,
            True,
            f"calibrated confidence {confidence:.2f} below auto-execute threshold",
        )
