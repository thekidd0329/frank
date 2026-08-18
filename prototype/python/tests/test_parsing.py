from pathlib import Path

from frank.parsing import is_semantic_opposite, parse_file


SANDBOX = Path('sandbox')


def test_parser_extracts_stale_profile_preference():
    ev = parse_file(SANDBOX / 'lexie_old.json')
    pref = next(e for e in ev if e.axis == 'aesthetic_preference')
    assert pref.value == 'minimalism'
    assert pref.polarity == 1
    assert 'lexie_likes_minimalism' in pref.supports


def test_parser_extracts_direct_contradiction():
    old = next(e for e in parse_file(SANDBOX / 'lexie_old.json') if e.axis == 'aesthetic_preference')
    messages = parse_file(SANDBOX / 'messages_lexie.json')
    newer = next(e for e in messages if e.axis == 'aesthetic_preference')
    assert newer.polarity == -1
    assert 'lexie_likes_minimalism' in newer.contradicts
    assert is_semantic_opposite(old, newer)


def test_parser_extracts_moon_and_drawing_history():
    posts = parse_file(SANDBOX / 'posts_lexie.json')
    history = parse_file(SANDBOX / 'user_history.json')
    assert any('moon_is_recurring_motif' in e.supports for e in posts)
    assert any('botanical_is_novel' in e.contradicts for e in history)
