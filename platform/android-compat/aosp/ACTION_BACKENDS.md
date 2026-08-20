# Frank Android 17 action backends

This adapter layer keeps Android execution mechanics outside Frank's cognitive substrate.

The Kotlin Residual Commitment Field and goal-control logic decide what is being attempted. The Rust control plane determines whether the caller may propose the action, whether the target app has any required network capability, whether explicit user confirmation is required, and which privileged executor backend is permitted.

## Backend order

For an app action, prefer the most structured mechanism available:

1. **AppFunctions backend** — use a registered typed app function when the target app exposes one.
2. **Computer Control backend** — use Android 17 Computer Control as a compatibility fallback for an unchanged app UI.
3. **Native broker backend** — use only for Frank/OS capabilities that are not really app UI operations, such as network policy or trusted device operations.

The Rust `ActionOrchestrator` implements this boundary without importing Android APIs. Android-specific implementations replace the reference backends behind the same contract.

## Computer Control adapter responsibilities

The production Android adapter must own all Computer Control-specific state and never expose raw control authority to cognition. It should map:

- privileged `ACCESS_COMPUTER_CONTROL` eligibility;
- `OP_COMPUTER_CONTROL` / consent state;
- one active control session at a time;
- the platform's sequential target-app limit;
- background virtual-display lifecycle;
- screenshot acquisition;
- tap, swipe, and text injection;
- target-app transition tracking;
- user takeover / confirmation handoff;
- session completion, cancellation, and timeout;
- normalized result events back to Frank.

A platform denial must remain a denial. The adapter must not fall back to AccessibilityService merely to bypass Computer Control eligibility or consent.

## AppFunctions adapter responsibilities

The production adapter should query the OS registry for callable functions, translate a Frank action target into the typed function invocation, validate arguments, invoke locally, and convert the result into a normalized action result.

AppFunctions are preferred over UI automation because the operation is explicit and machine-readable. They are not cognitive ground truth and must not be treated as evidence that Frank inherently understands an app's semantics.

## Consent boundary

Consent is evaluated before backend execution. A request marked `UserConfirmation` returns `NeedsConsent` until the user has actually confirmed. The backend must not start first and ask later.

For a Computer Control workflow, the Android adapter may additionally hand the live session to the user at a transaction boundary. That platform handoff is an execution mechanism; Frank's policy still decides which actions require that boundary.

## Network boundary

Any action that requires target-app networking may carry a `NetworkNeed(uid, capability)`. `ActionOrchestrator` verifies that the UID currently holds that explicit capability before starting the backend.

The action layer does not silently grant networking. Temporary grants, expiry, and eBPF/netd enforcement remain the responsibility of Frank's network-policy service.

## Security rule

`frank-modeld` should normally hold only `ProposeAction`.

`frank-actiond` may hold narrowly separated backend capabilities such as:

- `ExecuteAppFunction`
- `UseComputerControl`
- `ExecuteNativeBroker`

The model therefore cannot manufacture Computer Control authority by producing text that requests it.

## Current prototype status

`src/action.rs` is a dependency-free Linux-runnable reference implementation with mock backend adapters and tests. `src/bin/frank_actiond.rs` proves backend selection and policy separation. The files under `aosp/` are integration scaffolding; they do not claim that a stock non-OEM Android build grants Computer Control privileges.
