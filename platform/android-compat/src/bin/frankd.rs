use frank_android_compat::{
    Capability, CapabilityRequest, Decision, EventBus, NetworkBroker, NetworkPolicy, NormalizedEvent,
    PolicyEngine, Subject,
};

fn main() {
    let mut policy = PolicyEngine::new();
    let mut network = NetworkBroker::new();
    let mut events = EventBus::with_capacity(256);

    let model = Subject::FrankComponent("frank-modeld");
    let policy_daemon = Subject::FrankComponent("frank-policyd");

    policy
        .grant(model.clone(), Capability::ProposeAction)
        .expect("model proposal capability should be allowed");
    policy
        .deny_permanently(model.clone(), Capability::ChangeNetworkPolicy);
    policy
        .grant(policy_daemon.clone(), Capability::ChangeNetworkPolicy)
        .expect("policy daemon owns network-policy authority");

    let app_uid = 10342;
    policy
        .grant(Subject::AndroidUid(app_uid), Capability::Internet)
        .expect("demo WAN grant should succeed");

    let request = CapabilityRequest {
        subject: policy_daemon,
        capability: Capability::ChangeNetworkPolicy,
        rationale: "apply UID-scoped demo policy".into(),
    };

    if policy.decide(&request) == Decision::Allow {
        network.set_policy(
            app_uid,
            NetworkPolicy {
                internet: true,
                local_network: false,
                background: false,
                metered: true,
                expires_at: None,
            },
        );
    }

    events.publish(NormalizedEvent::NetworkAttempt {
        uid: app_uid,
        local: false,
        background: false,
    });

    println!("Frank Android compatibility control plane online");
    println!("uid {app_uid} network policy: {:?}", network.policy_for(app_uid));
    println!("normalized events queued: {}", events.len());
}
