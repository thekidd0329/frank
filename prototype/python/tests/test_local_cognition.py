from datetime import datetime, timedelta

from frank.local_cognition import InvestigationHypothesis, resolve_collision
from frank.models import Evidence


def ev(source: str, polarity: int, reliability: float = 0.9) -> Evidence:
    now = datetime.now()
    return Evidence(
        id=source,
        claim=source,
        source=source,
        reliability=reliability,
        freshness=1.0,
        content=source,
        timestamp=now - timedelta(hours=1),
        axis='aesthetic_preference',
        value='minimalism',
        polarity=polarity,
    )


def test_collision_freezes_but_third_source_can_thaw():
    h = InvestigationHypothesis('min_pref', 'Lexie prefers minimalism')
    assert resolve_collision(h, ev('profile', +1))
    assert resolve_collision(h, ev('messages', -1))
    assert h.frozen
    assert len(h.evidence) == 2

    assert resolve_collision(h, ev('posts', -1))
    assert not h.frozen
    assert len(h.evidence) == 3
