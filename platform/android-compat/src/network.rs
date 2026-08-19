use std::collections::HashMap;
use std::time::{Duration, Instant};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NetworkPolicy {
    pub internet: bool,
    pub local_network: bool,
    pub background: bool,
    pub metered: bool,
    pub expires_at: Option<Instant>,
}

impl Default for NetworkPolicy {
    fn default() -> Self {
        Self {
            internet: false,
            local_network: false,
            background: false,
            metered: false,
            expires_at: None,
        }
    }
}

#[derive(Debug, Default)]
pub struct NetworkBroker {
    policies: HashMap<u32, NetworkPolicy>,
}

impl NetworkBroker {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn set_policy(&mut self, uid: u32, policy: NetworkPolicy) {
        self.policies.insert(uid, policy);
    }

    pub fn set_temporary_policy(&mut self, uid: u32, mut policy: NetworkPolicy, ttl: Duration) {
        policy.expires_at = Some(Instant::now() + ttl);
        self.set_policy(uid, policy);
    }

    pub fn policy_for(&self, uid: u32) -> NetworkPolicy {
        match self.policies.get(&uid) {
            Some(policy) if !is_expired(policy) => policy.clone(),
            _ => NetworkPolicy::default(),
        }
    }

    pub fn purge_expired(&mut self) {
        self.policies.retain(|_, policy| !is_expired(policy));
    }
}

fn is_expired(policy: &NetworkPolicy) -> bool {
    policy.expires_at.is_some_and(|deadline| Instant::now() >= deadline)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn unknown_uid_is_default_deny() {
        let broker = NetworkBroker::new();
        assert_eq!(broker.policy_for(99999), NetworkPolicy::default());
    }

    #[test]
    fn policy_is_uid_scoped() {
        let mut broker = NetworkBroker::new();
        broker.set_policy(
            10342,
            NetworkPolicy {
                internet: true,
                metered: true,
                ..NetworkPolicy::default()
            },
        );

        assert!(broker.policy_for(10342).internet);
        assert!(!broker.policy_for(10343).internet);
        assert!(!broker.policy_for(10342).local_network);
    }
}
