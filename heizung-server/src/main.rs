use std::sync::{Arc, Mutex};
use valve_manager::ValveManager;

mod valve;
mod valve_manager;
mod schedule_manager;
mod network_control;
mod mqtt_client;

#[tokio::main]
async fn main() {
    println!("Starting Heizung-Server...");

    let valve_manager = Arc::new(Mutex::new(ValveManager::new().unwrap()));

    // For testing purposes, let's create a valve.
    // In the final version, this would be loaded from a config file.
    {
        let mut vm = valve_manager.lock().unwrap();
        vm.create_valve(17, "Living Room").unwrap();
    }

    println!("ValveManager created and valve initialized.");

    // The rest of the application logic will go here.
    // For now, we'll just keep the application running.
    loop {
        tokio::time::sleep(tokio::time::Duration::from_secs(1)).await;
    }
}
