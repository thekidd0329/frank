#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ControlInput {
    Tap { x: i32, y: i32 },
    Swipe {
        from_x: i32,
        from_y: i32,
        to_x: i32,
        to_y: i32,
    },
    Text(String),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ComputerControlSession {
    pub id: u64,
    pub targets: Vec<String>,
    pub handed_to_user: bool,
    pub screenshot_count: u64,
    pub input_count: u64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SessionError {
    SessionAlreadyActive,
    NoActiveSession,
    TargetLimitReached,
    UserHasControl,
}

#[derive(Debug)]
pub struct ComputerControlSessionManager {
    active: Option<ComputerControlSession>,
    next_id: u64,
    max_sequential_targets: usize,
}

impl Default for ComputerControlSessionManager {
    fn default() -> Self {
        Self {
            active: None,
            next_id: 1,
            max_sequential_targets: 6,
        }
    }
}

impl ComputerControlSessionManager {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn active(&self) -> Option<&ComputerControlSession> {
        self.active.as_ref()
    }

    pub fn start(&mut self, package: impl Into<String>) -> Result<u64, SessionError> {
        if self.active.is_some() {
            return Err(SessionError::SessionAlreadyActive);
        }
        let id = self.next_id;
        self.next_id = self.next_id.saturating_add(1);
        self.active = Some(ComputerControlSession {
            id,
            targets: vec![package.into()],
            handed_to_user: false,
            screenshot_count: 0,
            input_count: 0,
        });
        Ok(id)
    }

    pub fn add_target(&mut self, package: impl Into<String>) -> Result<(), SessionError> {
        let session = self.active.as_mut().ok_or(SessionError::NoActiveSession)?;
        if session.targets.len() >= self.max_sequential_targets {
            return Err(SessionError::TargetLimitReached);
        }
        session.targets.push(package.into());
        Ok(())
    }

    pub fn record_screenshot(&mut self) -> Result<u64, SessionError> {
        let session = self.active.as_mut().ok_or(SessionError::NoActiveSession)?;
        if session.handed_to_user {
            return Err(SessionError::UserHasControl);
        }
        session.screenshot_count = session.screenshot_count.saturating_add(1);
        Ok(session.screenshot_count)
    }

    pub fn inject(&mut self, _input: ControlInput) -> Result<u64, SessionError> {
        let session = self.active.as_mut().ok_or(SessionError::NoActiveSession)?;
        if session.handed_to_user {
            return Err(SessionError::UserHasControl);
        }
        session.input_count = session.input_count.saturating_add(1);
        Ok(session.input_count)
    }

    pub fn handoff_to_user(&mut self) -> Result<(), SessionError> {
        let session = self.active.as_mut().ok_or(SessionError::NoActiveSession)?;
        session.handed_to_user = true;
        Ok(())
    }

    pub fn resume_automation(&mut self) -> Result<(), SessionError> {
        let session = self.active.as_mut().ok_or(SessionError::NoActiveSession)?;
        session.handed_to_user = false;
        Ok(())
    }

    pub fn finish(&mut self) -> Result<ComputerControlSession, SessionError> {
        self.active.take().ok_or(SessionError::NoActiveSession)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn only_one_session_can_be_active() {
        let mut manager = ComputerControlSessionManager::new();
        manager.start("com.example.one").unwrap();
        assert_eq!(
            manager.start("com.example.two"),
            Err(SessionError::SessionAlreadyActive)
        );
        manager.finish().unwrap();
        assert!(manager.start("com.example.two").is_ok());
    }

    #[test]
    fn sequential_target_limit_is_enforced() {
        let mut manager = ComputerControlSessionManager::new();
        manager.start("app.1").unwrap();
        for index in 2..=6 {
            manager.add_target(format!("app.{index}")).unwrap();
        }
        assert_eq!(
            manager.add_target("app.7"),
            Err(SessionError::TargetLimitReached)
        );
        assert_eq!(manager.active().unwrap().targets.len(), 6);
    }

    #[test]
    fn user_handoff_blocks_automation_until_resumed() {
        let mut manager = ComputerControlSessionManager::new();
        manager.start("com.example.pay").unwrap();
        manager.record_screenshot().unwrap();
        manager
            .inject(ControlInput::Tap { x: 10, y: 20 })
            .unwrap();
        manager.handoff_to_user().unwrap();
        assert_eq!(
            manager.inject(ControlInput::Text("confirm".into())),
            Err(SessionError::UserHasControl)
        );
        assert_eq!(manager.record_screenshot(), Err(SessionError::UserHasControl));
        manager.resume_automation().unwrap();
        assert!(manager.inject(ControlInput::Text("continue".into())).is_ok());
    }

    #[test]
    fn session_records_control_activity_without_storing_pixels() {
        let mut manager = ComputerControlSessionManager::new();
        manager.start("com.example.maps").unwrap();
        assert_eq!(manager.record_screenshot().unwrap(), 1);
        assert_eq!(manager.record_screenshot().unwrap(), 2);
        assert_eq!(
            manager
                .inject(ControlInput::Swipe {
                    from_x: 0,
                    from_y: 100,
                    to_x: 0,
                    to_y: 0,
                })
                .unwrap(),
            1
        );
        let finished = manager.finish().unwrap();
        assert_eq!(finished.screenshot_count, 2);
        assert_eq!(finished.input_count, 1);
    }
}
