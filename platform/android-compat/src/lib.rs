//! Frank Android compatibility control plane.
//!
//! The central rule is deliberately boring: cognition may propose actions,
//! but only deterministic policy code may authorize privileged operations.

pub mod event;
pub mod network;
pub mod policy;
pub mod trust;

pub use event::{EventBus, NormalizedEvent};
pub use network::{NetworkBroker, NetworkPolicy};
pub use policy::{Capability, CapabilityRequest, Decision, PolicyEngine, Subject};
pub use trust::{InMemoryTrustRoot, TrustRoot};
