//! Manages the heating schedule.

use crate::valve_manager::ValveManager;
use std::sync::{Arc, Mutex};

pub struct ScheduleManager {
    valve_manager: Arc<Mutex<ValveManager>>,
}

impl ScheduleManager {
    pub fn new(valve_manager: Arc<Mutex<ValveManager>>) -> Self {
        Self { valve_manager }
    }

    pub async fn start(&self) {
        // Implementation to come
    }
}
