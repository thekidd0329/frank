use frank_android_compat::{
    ActionBackendKind, ActionOrchestrator, ActionRequest, ActionStatus, ActionTarget,
    AppFunctionsBackend, Capability, ComputerControlBackend, ConsentRequirement,
    NativeBrokerBackend, PolicyEngine, Subject,
};

fn main() {
    let model = Subject::FrankComponent("frank-modeld");
    let actiond = Subject::FrankComponent("frank-actiond");

    let mut policy = PolicyEngine::new();
    policy.grant(model.clone(), Capability::ProposeAction).expect("grant proposal");
    policy
        .grant(actiond.clone(), Capability::ExecuteAppFunction)
        .expect("grant app-function backend");
    policy
        .grant(actiond.clone(), Capability::UseComputerControl)
        .expect("grant computer-control backend");
    policy
        .grant(actiond.clone(), Capability::ExecuteNativeBroker)
        .expect("grant native broker backend");

    let orchestrator = ActionOrchestrator::new(actiond)
        .with_backend(
            AppFunctionsBackend::new().register("com.example.maps", "start_navigation"),
        )
        .with_backend(ComputerControlBackend::new().allow_package("com.example.maps"))
        .with_backend(NativeBrokerBackend::new().register("network.policy.set"));

    let request = ActionRequest {
        caller: model,
        target: ActionTarget::App {
            package: "com.example.maps".into(),
            function: Some("start_navigation".into()),
        },
        rationale: "demonstrate deterministic backend routing".into(),
        consent_requirement: ConsentRequirement::None,
        consent_granted: false,
        network_need: None,
    };

    let result = orchestrator.execute(&policy, &request);
    assert_eq!(result.status, ActionStatus::Executed);
    assert_eq!(result.backend, Some(ActionBackendKind::AppFunctions));
    println!("frank-actiond reference route: {:?}", result.backend);
}
