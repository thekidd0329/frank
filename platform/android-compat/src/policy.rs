use std::collections::{HashMap, HashSet};

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum Subject {
    AndroidUid(u32),
    FrankComponent(&'static str),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Capability {
    Internet,
    LocalNetwork,
    BackgroundNetwork,
    ReadSemanticMemory,
    ProposeAction,
    ExecuteAppFunction,
    UseComputerControl,
    ExecuteNativeBroker,
    ChangeNetworkPolicy,
    ReadTrustRoot,
    WriteTrustRoot,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CapabilityRequest {
    pub subject: Subject,
    pub capability: Capability,
    pub rationale: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Decision {
    Allow,
    Deny(&'static str),
}

#[derive(Debug, Default)]
pub struct PolicyEngine {
    grants: HashMap<Subject, HashSet<Capability>>,
    immutable_denies: HashMap<Subject, HashSet<Capability>>,
}

impl PolicyEngine {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn grant(&mut self, subject: Subject, capability: Capability) -> Result<(), &'static str> {
        if self
            .immutable_denies
            .get(&subject)
            .is_some_and(|denies| denies.contains(&capability))
        {
            return Err("capability is constitutionally denied");
        }
        self.grants.entry(subject).or_default().insert(capability);
        Ok(())
    }

    pub fn revoke(&mut self, subject: &Subject, capability: Capability) {
        if let Some(grants) = self.grants.get_mut(subject) {
            grants.remove(&capability);
        }
    }

    pub fn deny_permanently(&mut self, subject: Subject, capability: Capability) {
        self.revoke(&subject, capability);
        self.immutable_denies.entry(subject).or_default().insert(capability);
    }

    pub fn decide(&self, request: &CapabilityRequest) -> Decision {
        if self
            .immutable_denies
            .get(&request.subject)
            .is_some_and(|denies| denies.contains(&request.capability))
        {
            return Decision::Deny("blocked by immutable policy");
        }

        if self
            .grants
            .get(&request.subject)
            .is_some_and(|grants| grants.contains(&request.capability))
        {
            Decision::Allow
        } else {
            Decision::Deny("capability not granted")
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cognition_cannot_self_grant_network_authority() {
        let model = Subject::FrankComponent("frank-modeld");
        let mut policy = PolicyEngine::new();
        policy.deny_permanently(model.clone(), Capability::ChangeNetworkPolicy);

        assert_eq!(
            policy.grant(model.clone(), Capability::ChangeNetworkPolicy),
            Err("capability is constitutionally denied")
        );
        assert!(matches!(
            policy.decide(&CapabilityRequest {
                subject: model,
                capability: Capability::ChangeNetworkPolicy,
                rationale: "prompt requested it".into(),
            }),
            Decision::Deny(_)
        ));
    }

    #[test]
    fn explicit_grant_allows_only_named_capability() {
        let app = Subject::AndroidUid(10342);
        let mut policy = PolicyEngine::new();
        policy.grant(app.clone(), Capability::Internet).unwrap();

        assert_eq!(
            policy.decide(&CapabilityRequest {
                subject: app.clone(),
                capability: Capability::Internet,
                rationale: "user enabled WAN".into(),
            }),
            Decision::Allow
        );
        assert!(matches!(
            policy.decide(&CapabilityRequest {
                subject: app,
                capability: Capability::LocalNetwork,
                rationale: "LAN discovery".into(),
            }),
            Decision::Deny(_)
        ));
    }

    #[test]
    fn action_capabilities_are_independent() {
        let actiond = Subject::FrankComponent("frank-actiond");
        let mut policy = PolicyEngine::new();
        policy.grant(actiond.clone(), Capability::ExecuteAppFunction).unwrap();

        assert_eq!(
            policy.decide(&CapabilityRequest {
                subject: actiond.clone(),
                capability: Capability::ExecuteAppFunction,
                rationale: "structured app action".into(),
            }),
            Decision::Allow
        );
        assert!(matches!(
            policy.decide(&CapabilityRequest {
                subject: actiond,
                capability: Capability::UseComputerControl,
                rationale: "UI fallback".into(),
            }),
            Decision::Deny(_)
        ));
    }
}
