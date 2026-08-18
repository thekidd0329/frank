from frank.lexie_benchmark import run_benchmark


if __name__ == "__main__":
    state = run_benchmark("sandbox")
    print(f"steps={state.step}")
    print(f"evidence={len(state.evidence)}")
    print(state.final_answer)
