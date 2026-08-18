from frank.authority import AuthorityFirewall
from frank.models import ActionCandidate, SideEffect


def test_readonly_action_does_not_require_confirmation():
    firewall = AuthorityFirewall(confirm_all_external=True)
    action = ActionCandidate("search", {}, 0.5, side_effect=SideEffect.NONE)
    decision = firewall.evaluate(action)
    assert decision.allowed
    assert not decision.confirmation_required


def test_external_action_hits_confirmation_firewall_without_changing_cognition():
    firewall = AuthorityFirewall(confirm_all_external=True)
    action = ActionCandidate("send_message", {"to": "Nancy"}, 0.0, side_effect=SideEffect.EXTERNAL)
    decision = firewall.evaluate(action)
    assert not decision.allowed
    assert decision.confirmation_required
