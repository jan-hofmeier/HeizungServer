//! Manages all the valves in the heating system.

use crate::valve::Valve;
use anyhow::{anyhow, Result};
use rppal::gpio::{Gpio, OutputPin};
use std::collections::HashMap;

pub struct ValveManager {
    // We don't need the Gpio object after initialization, but we keep it here
    // so that it's dropped when ValveManager is dropped, which handles cleanup.
    _gpio: Gpio,
    valves_by_pin: HashMap<u8, (Valve, OutputPin)>,
    valves_by_name: HashMap<String, u8>, // Look up pin by name
}

impl ValveManager {
    /// Creates a new ValveManager. This initializes the GPIO interface.
    pub fn new() -> Result<Self> {
        Ok(Self {
            _gpio: Gpio::new()?,
            valves_by_pin: HashMap::new(),
            valves_by_name: HashMap::new(),
        })
    }

    /// Creates and configures a new valve.
    pub fn create_valve(&mut self, pin_num: u8, name: &str) -> Result<()> {
        if self.valves_by_name.contains_key(name) {
            return Err(anyhow!("Valve with name '{}' already exists", name));
        }
        if self.valves_by_pin.contains_key(&pin_num) {
            return Err(anyhow!("Valve at pin {} already exists", pin_num));
        }

        let mut pin = self._gpio.get(pin_num)?.into_output();
        // The original Java code set `active_low` to true, which inverts the logic.
        // So, to turn the valve OFF, we set the pin HIGH.
        pin.set_high();

        let valve = Valve {
            pin: pin_num,
            name: name.to_string(),
            is_on: false,
            current_temp: 0.0,
            current_humidity: 0.0,
        };

        self.valves_by_pin.insert(pin_num, (valve, pin));
        self.valves_by_name.insert(name.to_string(), pin_num);

        Ok(())
    }

    pub fn get_valve_by_pin(&self, pin: u8) -> Option<&Valve> {
        self.valves_by_pin.get(&pin).map(|(v, _p)| v)
    }

    pub fn get_valve_by_name(&self, name: &str) -> Option<&Valve> {
        self.valves_by_name
            .get(name)
            .and_then(|pin| self.get_valve_by_pin(*pin))
    }

    /// Sets the temperature for a room, updating all valves associated with it.
    pub fn set_temperature(&mut self, room: &str, temp: f32) {
        for (valve, _pin) in self.valves_by_pin.values_mut() {
            if valve.name.starts_with(room) {
                valve.current_temp = temp;
            }
        }
    }

    /// Sets the humidity for a room, updating all valves associated with it.
    pub fn set_humidity(&mut self, room: &str, humidity: f32) {
        for (valve, _pin) in self.valves_by_pin.values_mut() {
            if valve.name.starts_with(room) {
                valve.current_humidity = humidity;
            }
        }
    }

    /// Turns valves on or off based on a list of active valve names.
    pub fn set_active_valves(&mut self, active_valves: &[String]) {
        for (valve, pin) in self.valves_by_pin.values_mut() {
            let should_be_on = active_valves.contains(&valve.name);
            if valve.is_on != should_be_on {
                if should_be_on {
                    // To turn ON, set pin LOW (due to `active_low` inversion).
                    pin.set_low();
                } else {
                    // To turn OFF, set pin HIGH.
                    pin.set_high();
                }
                valve.is_on = should_be_on;
            }
        }
    }

    /// Returns a JSON representation of the current state of all valves.
    pub fn to_json(&self) -> Result<String> {
        let valve_map: HashMap<_, _> = self
            .valves_by_pin
            .values()
            .map(|(v, _p)| (v.name.clone(), v))
            .collect();
        Ok(serde_json::to_string(&valve_map)?)
    }
}
