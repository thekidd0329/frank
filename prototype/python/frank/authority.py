from __future__ import annotations

from dataclasses import dataclass

from .models import ActionCandidate, SideEffect


@dataclass(slots=True)
class AuthorityDecision:
    allowed: bool
    confirmation_required: bool
    reason: str


class AuthorityFirewall:
    """Policy boundary only. It does not participate in Frank's reasoning loop."""

    def __init__(self, confirm_all_external: bool = True):
        self.confirm_all_external = confirm_all_external

    def evaluate(self, action: ActionCandidate) -> AuthorityDecision:
        if action.side_effect is SideEffect.NONE:
            return AuthorityDecision(True, False, "read-only/internal action")

        if self.confirm_all_external:
            return AuthorityDecision(False, True, "external side effect requires owner confirmation")

        return AuthorityDecision(True, False, "policy permits autonomous execution")
