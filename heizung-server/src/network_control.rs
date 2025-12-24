//! Handles the HTTP server.

use crate::valve_manager::ValveManager;
use std::sync::{Arc, Mutex};
use axum::{routing::get, Router};

pub async fn start_server(valve_manager: Arc<Mutex<ValveManager>>) {
    let app = Router::new()
        .route("/", get(handler));

    let listener = tokio::net::TcpListener::bind("0.0.0.0:80").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn handler() -> &'static str {
    "Hello, World!"
}
