//! Frank Android compatibility control plane.
//!
//! Cognition remains in the existing Kotlin Residual Commitment Field.
//! This Rust crate is intentionally limited to deterministic OS authority:
//! policy, action routing, networking, normalized events, and trust-root integration.

pub mod action;
pub mod computer_control;
pub mod event;
pub mod network;
pub mod policy;
pub mod trust;

pub use action::{
    ActionBackend, ActionBackendKind, ActionExecution, ActionOrchestrator, ActionRequest, ActionStatus,
    ActionTarget, AppFunctionsBackend, ComputerControlBackend, ConsentRequirement, NativeBrokerBackend,
    NetworkNeed,
};
pub use computer_control::{
    ComputerControlSession, ComputerControlSessionManager, ControlInput, SessionError,
};
pub use event::{EventBus, NormalizedEvent};
pub use network::{NetworkBroker, NetworkPolicy};
pub use policy::{Capability, CapabilityRequest, Decision, PolicyEngine, Subject};
pub use trust::{InMemoryTrustRoot, TrustRoot};
