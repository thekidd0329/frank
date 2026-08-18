from frank.authority import AuthorityFirewall
from frank.models import ActionCandidate, SideEffect


def test_readonly_action_does_not_require_confirmation():
    firewall = AuthorityFirewall()
    action = ActionCandidate("search", {}, 0.5, side_effect=SideEffect.NONE)
    decision = firewall.evaluate(action)
    assert decision.allowed
    assert not decision.confirmation_required


def test_external_action_requires_confirmation_below_threshold():
    firewall = AuthorityFirewall(auto_execute_threshold=0.90)
    action = ActionCandidate("send_message", {"to": "Nancy"}, 0.0, side_effect=SideEffect.EXTERNAL)
    decision = firewall.evaluate(
        action,
        calibrated_confidence=0.89,
        scope_permitted=True,
    )
    assert not decision.allowed
    assert decision.confirmation_required


def test_external_action_auto_executes_at_threshold():
    firewall = AuthorityFirewall(auto_execute_threshold=0.90)
    action = ActionCandidate("send_message", {"to": "Nancy"}, 0.0, side_effect=SideEffect.EXTERNAL)
    decision = firewall.evaluate(
        action,
        calibrated_confidence=0.90,
        scope_permitted=True,
    )
    assert decision.allowed
    assert not decision.confirmation_required


def test_scope_blocks_even_above_threshold():
    firewall = AuthorityFirewall(auto_execute_threshold=0.90)
    action = ActionCandidate("send_message", {"to": "Nancy"}, 0.0, side_effect=SideEffect.EXTERNAL)
    decision = firewall.evaluate(
        action,
        calibrated_confidence=0.99,
        scope_permitted=False,
    )
    assert not decision.allowed
    assert not decision.confirmation_required
