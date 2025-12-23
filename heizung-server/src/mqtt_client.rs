//! Handles the MQTT client.

use crate::valve_manager::ValveManager;
use std::sync::{Arc, Mutex};

pub struct MqttClient {
    valve_manager: Arc<Mutex<ValveManager>>,
}

impl MqttClient {
    pub fn new(valve_manager: Arc<Mutex<ValveManager>>) -> Self {
        Self { valve_manager }
    }

    pub async fn start(&self) {
        // Implementation to come
    }
}
