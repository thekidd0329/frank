from frank.lexie_benchmark import run_benchmark


def test_lexie_benchmark_rejects_stale_and_duplicate_concepts():
    state = run_benchmark("sandbox")
    assert state.done
    assert state.step >= 3
    assert state.final_answer is not None
    answer = state.final_answer.lower()
    assert "moon" in answer
    assert "minimalist" in answer
    assert "botanical" in answer


def test_benchmark_collects_contradictory_evidence():
    state = run_benchmark("sandbox")
    supports = [e for e in state.evidence.values() if "lexie_likes_minimalism" in e.supports]
    contradicts = [e for e in state.evidence.values() if "lexie_likes_minimalism" in e.contradicts]
    assert supports
    assert contradicts
