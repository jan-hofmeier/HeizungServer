//! Represents a single valve in the heating system.

#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct Valve {
    pub pin: u8,
    pub name: String,
    pub is_on: bool,
    pub current_temp: f32,
    pub current_humidity: f32,
}
