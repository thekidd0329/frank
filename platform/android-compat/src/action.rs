use crate::policy::{Capability, CapabilityRequest, Decision, PolicyEngine, Subject};
use std::collections::HashSet;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ActionBackendKind {
    AppFunctions,
    ComputerControl,
    NativeBroker,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ActionTarget {
    App {
        package: String,
        function: Option<String>,
    },
    Native {
        capability_id: String,
    },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ConsentRequirement {
    None,
    UserConfirmation,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NetworkNeed {
    pub uid: u32,
    pub capability: Capability,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ActionRequest {
    pub caller: Subject,
    pub target: ActionTarget,
    pub rationale: String,
    pub consent_requirement: ConsentRequirement,
    pub consent_granted: bool,
    pub network_need: Option<NetworkNeed>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ActionStatus {
    Executed,
    NeedsConsent,
    Denied,
    Unsupported,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ActionExecution {
    pub backend: Option<ActionBackendKind>,
    pub status: ActionStatus,
    pub detail: String,
}

pub trait ActionBackend: Send + Sync {
    fn kind(&self) -> ActionBackendKind;
    fn supports(&self, request: &ActionRequest) -> bool;
    fn execute(&self, request: &ActionRequest) -> ActionExecution;
}

pub struct ActionOrchestrator {
    executor: Subject,
    backends: Vec<Box<dyn ActionBackend>>,
}

impl ActionOrchestrator {
    pub fn new(executor: Subject) -> Self {
        Self {
            executor,
            backends: Vec::new(),
        }
    }

    pub fn with_backend(mut self, backend: impl ActionBackend + 'static) -> Self {
        self.backends.push(Box::new(backend));
        self
    }

    pub fn execute(&self, policy: &PolicyEngine, request: &ActionRequest) -> ActionExecution {
        if !allowed(
            policy,
            request.caller.clone(),
            Capability::ProposeAction,
            &request.rationale,
        ) {
            return denied("caller may not propose actions");
        }

        if let Some(network) = &request.network_need {
            if !matches!(
                network.capability,
                Capability::Internet | Capability::LocalNetwork | Capability::BackgroundNetwork
            ) {
                return denied("network requirement is not a network capability");
            }
            if !allowed(
                policy,
                Subject::AndroidUid(network.uid),
                network.capability,
                "target app network requirement",
            ) {
                return denied("target app lacks required network capability");
            }
        }

        if request.consent_requirement == ConsentRequirement::UserConfirmation && !request.consent_granted {
            return ActionExecution {
                backend: None,
                status: ActionStatus::NeedsConsent,
                detail: "explicit user confirmation required".into(),
            };
        }

        let mut supported_but_denied = false;
        for backend in &self.backends {
            if !backend.supports(request) {
                continue;
            }

            let capability = backend_capability(backend.kind());
            if !allowed(
                policy,
                self.executor.clone(),
                capability,
                "authorized action backend execution",
            ) {
                supported_but_denied = true;
                continue;
            }

            return backend.execute(request);
        }

        if supported_but_denied {
            denied("a compatible backend exists but its executor capability is denied")
        } else {
            ActionExecution {
                backend: None,
                status: ActionStatus::Unsupported,
                detail: "no registered backend supports this action".into(),
            }
        }
    }
}

fn backend_capability(kind: ActionBackendKind) -> Capability {
    match kind {
        ActionBackendKind::AppFunctions => Capability::ExecuteAppFunction,
        ActionBackendKind::ComputerControl => Capability::UseComputerControl,
        ActionBackendKind::NativeBroker => Capability::ExecuteNativeBroker,
    }
}

fn allowed(policy: &PolicyEngine, subject: Subject, capability: Capability, rationale: &str) -> bool {
    matches!(
        policy.decide(&CapabilityRequest {
            subject,
            capability,
            rationale: rationale.into(),
        }),
        Decision::Allow
    )
}

fn denied(detail: &str) -> ActionExecution {
    ActionExecution {
        backend: None,
        status: ActionStatus::Denied,
        detail: detail.into(),
    }
}

#[derive(Debug, Default)]
pub struct AppFunctionsBackend {
    functions: HashSet<(String, String)>,
}

impl AppFunctionsBackend {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn register(mut self, package: impl Into<String>, function: impl Into<String>) -> Self {
        self.functions.insert((package.into(), function.into()));
        self
    }
}

impl ActionBackend for AppFunctionsBackend {
    fn kind(&self) -> ActionBackendKind {
        ActionBackendKind::AppFunctions
    }

    fn supports(&self, request: &ActionRequest) -> bool {
        match &request.target {
            ActionTarget::App {
                package,
                function: Some(function),
            } => self.functions.contains(&(package.clone(), function.clone())),
            _ => false,
        }
    }

    fn execute(&self, request: &ActionRequest) -> ActionExecution {
        ActionExecution {
            backend: Some(self.kind()),
            status: ActionStatus::Executed,
            detail: format!("structured app function accepted: {:?}", request.target),
        }
    }
}

#[derive(Debug)]
pub struct ComputerControlBackend {
    packages: HashSet<String>,
    pub max_sequential_targets: usize,
}

impl Default for ComputerControlBackend {
    fn default() -> Self {
        Self {
            packages: HashSet::new(),
            max_sequential_targets: 6,
        }
    }
}

impl ComputerControlBackend {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn allow_package(mut self, package: impl Into<String>) -> Self {
        self.packages.insert(package.into());
        self
    }
}

impl ActionBackend for ComputerControlBackend {
    fn kind(&self) -> ActionBackendKind {
        ActionBackendKind::ComputerControl
    }

    fn supports(&self, request: &ActionRequest) -> bool {
        match &request.target {
            ActionTarget::App { package, .. } => self.packages.contains(package),
            _ => false,
        }
    }

    fn execute(&self, request: &ActionRequest) -> ActionExecution {
        ActionExecution {
            backend: Some(self.kind()),
            status: ActionStatus::Executed,
            detail: format!("computer-control session requested for {:?}", request.target),
        }
    }
}

#[derive(Debug, Default)]
pub struct NativeBrokerBackend {
    capabilities: HashSet<String>,
}

impl NativeBrokerBackend {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn register(mut self, capability_id: impl Into<String>) -> Self {
        self.capabilities.insert(capability_id.into());
        self
    }
}

impl ActionBackend for NativeBrokerBackend {
    fn kind(&self) -> ActionBackendKind {
        ActionBackendKind::NativeBroker
    }

    fn supports(&self, request: &ActionRequest) -> bool {
        match &request.target {
            ActionTarget::Native { capability_id } => self.capabilities.contains(capability_id),
            _ => false,
        }
    }

    fn execute(&self, request: &ActionRequest) -> ActionExecution {
        ActionExecution {
            backend: Some(self.kind()),
            status: ActionStatus::Executed,
            detail: format!("native broker accepted: {:?}", request.target),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn base_policy() -> PolicyEngine {
        let mut policy = PolicyEngine::new();
        policy
            .grant(Subject::FrankComponent("frank-modeld"), Capability::ProposeAction)
            .unwrap();
        policy
            .grant(Subject::FrankComponent("frank-actiond"), Capability::ExecuteAppFunction)
            .unwrap();
        policy
            .grant(Subject::FrankComponent("frank-actiond"), Capability::UseComputerControl)
            .unwrap();
        policy
            .grant(Subject::FrankComponent("frank-actiond"), Capability::ExecuteNativeBroker)
            .unwrap();
        policy
    }

    fn app_request(function: Option<&str>) -> ActionRequest {
        ActionRequest {
            caller: Subject::FrankComponent("frank-modeld"),
            target: ActionTarget::App {
                package: "com.example.maps".into(),
                function: function.map(str::to_string),
            },
            rationale: "advance current goal".into(),
            consent_requirement: ConsentRequirement::None,
            consent_granted: false,
            network_need: None,
        }
    }

    #[test]
    fn structured_action_prefers_app_functions() {
        let policy = base_policy();
        let router = ActionOrchestrator::new(Subject::FrankComponent("frank-actiond"))
            .with_backend(AppFunctionsBackend::new().register("com.example.maps", "start_navigation"))
            .with_backend(ComputerControlBackend::new().allow_package("com.example.maps"));

        let result = router.execute(&policy, &app_request(Some("start_navigation")));
        assert_eq!(result.status, ActionStatus::Executed);
        assert_eq!(result.backend, Some(ActionBackendKind::AppFunctions));
    }

    #[test]
    fn computer_control_is_ui_fallback() {
        let policy = base_policy();
        let router = ActionOrchestrator::new(Subject::FrankComponent("frank-actiond"))
            .with_backend(AppFunctionsBackend::new())
            .with_backend(ComputerControlBackend::new().allow_package("com.example.maps"));

        let result = router.execute(&policy, &app_request(None));
        assert_eq!(result.status, ActionStatus::Executed);
        assert_eq!(result.backend, Some(ActionBackendKind::ComputerControl));
    }

    #[test]
    fn consent_boundary_stops_execution_before_backend() {
        let policy = base_policy();
        let router = ActionOrchestrator::new(Subject::FrankComponent("frank-actiond"))
            .with_backend(ComputerControlBackend::new().allow_package("com.example.maps"));
        let mut request = app_request(None);
        request.consent_requirement = ConsentRequirement::UserConfirmation;

        let result = router.execute(&policy, &request);
        assert_eq!(result.status, ActionStatus::NeedsConsent);
        assert_eq!(result.backend, None);
    }

    #[test]
    fn target_network_capability_is_checked() {
        let mut policy = base_policy();
        let router = ActionOrchestrator::new(Subject::FrankComponent("frank-actiond"))
            .with_backend(ComputerControlBackend::new().allow_package("com.example.maps"));
        let mut request = app_request(None);
        request.network_need = Some(NetworkNeed {
            uid: 10342,
            capability: Capability::Internet,
        });

        assert_eq!(router.execute(&policy, &request).status, ActionStatus::Denied);
        policy.grant(Subject::AndroidUid(10342), Capability::Internet).unwrap();
        assert_eq!(router.execute(&policy, &request).status, ActionStatus::Executed);
    }

    #[test]
    fn cognition_without_proposal_capability_cannot_reach_backend() {
        let policy = PolicyEngine::new();
        let router = ActionOrchestrator::new(Subject::FrankComponent("frank-actiond"))
            .with_backend(ComputerControlBackend::new().allow_package("com.example.maps"));
        assert_eq!(router.execute(&policy, &app_request(None)).status, ActionStatus::Denied);
    }

    #[test]
    fn native_actions_route_only_to_native_broker() {
        let policy = base_policy();
        let router = ActionOrchestrator::new(Subject::FrankComponent("frank-actiond"))
            .with_backend(AppFunctionsBackend::new())
            .with_backend(ComputerControlBackend::new())
            .with_backend(NativeBrokerBackend::new().register("network.policy.set"));
        let request = ActionRequest {
            caller: Subject::FrankComponent("frank-modeld"),
            target: ActionTarget::Native {
                capability_id: "network.policy.set".into(),
            },
            rationale: "apply approved OS policy".into(),
            consent_requirement: ConsentRequirement::None,
            consent_granted: false,
            network_need: None,
        };

        let result = router.execute(&policy, &request);
        assert_eq!(result.backend, Some(ActionBackendKind::NativeBroker));
        assert_eq!(result.status, ActionStatus::Executed);
    }
}
