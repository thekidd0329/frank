from __future__ import annotations

from dataclasses import dataclass
from typing import Callable

from .models import ActionCandidate, CognitiveState, Observation, SideEffect


@dataclass(slots=True)
class PlannerConfig:
    max_steps: int = 12
    min_information_gain: float = 0.05


class AutonomousPlanner:
    """
    Generic autonomous cognition loop.

    The planner is intentionally policy-agnostic: it reasons until it believes
    the goal is resolved. External authority is evaluated only after a final
    action is proposed.
    """

    def __init__(
        self,
        propose_next: Callable[[CognitiveState], ActionCandidate | None],
        execute_readonly: Callable[[ActionCandidate], object],
        integrate_observation: Callable[[CognitiveState, Observation], None],
        should_stop: Callable[[CognitiveState], bool],
        config: PlannerConfig | None = None,
    ):
        self.propose_next = propose_next
        self.execute_readonly = execute_readonly
        self.integrate_observation = integrate_observation
        self.should_stop = should_stop
        self.config = config or PlannerConfig()

    def run(self, state: CognitiveState) -> CognitiveState:
        while state.step < self.config.max_steps:
            if self.should_stop(state):
                state.done = True
                return state

            action = self.propose_next(state)
            if action is None:
                state.done = True
                return state

            if action.expected_information_gain < self.config.min_information_gain:
                state.done = True
                return state

            if action.side_effect is SideEffect.EXTERNAL:
                state.proposed_external_action = action
                state.done = True
                return state

            try:
                result = self.execute_readonly(action)
                obs = Observation(action=action, result=result, success=True)
            except Exception as exc:  # recovery path is evidence too
                obs = Observation(action=action, result=None, success=False, error=str(exc))

            state.observations.append(obs)
            self.integrate_observation(state, obs)
            state.step += 1

        state.done = True
        return state
