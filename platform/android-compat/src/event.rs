use std::collections::VecDeque;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum NormalizedEvent {
    ProcessStarted { uid: u32, process: String },
    ForegroundChanged { uid: u32, package: String },
    NetworkAttempt { uid: u32, local: bool, background: bool },
    AudioSessionChanged { uid: u32, active: bool },
    DisplayStateChanged { interactive: bool },
    BluetoothChanged { connected: bool, device_class: String },
}

#[derive(Debug)]
pub struct EventBus {
    queue: VecDeque<NormalizedEvent>,
    capacity: usize,
}

impl EventBus {
    pub fn with_capacity(capacity: usize) -> Self {
        assert!(capacity > 0, "event bus capacity must be non-zero");
        Self {
            queue: VecDeque::with_capacity(capacity),
            capacity,
        }
    }

    pub fn publish(&mut self, event: NormalizedEvent) {
        if self.queue.len() == self.capacity {
            self.queue.pop_front();
        }
        self.queue.push_back(event);
    }

    pub fn drain(&mut self) -> impl Iterator<Item = NormalizedEvent> + '_ {
        self.queue.drain(..)
    }

    pub fn len(&self) -> usize {
        self.queue.len()
    }

    pub fn is_empty(&self) -> bool {
        self.queue.is_empty()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bus_is_bounded() {
        let mut bus = EventBus::with_capacity(2);
        bus.publish(NormalizedEvent::DisplayStateChanged { interactive: true });
        bus.publish(NormalizedEvent::DisplayStateChanged { interactive: false });
        bus.publish(NormalizedEvent::AudioSessionChanged { uid: 10001, active: true });

        assert_eq!(bus.len(), 2);
        let events: Vec<_> = bus.drain().collect();
        assert!(matches!(events[0], NormalizedEvent::DisplayStateChanged { interactive: false }));
    }
}
