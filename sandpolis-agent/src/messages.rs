use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct UninstallRequest;

#[derive(Serialize, Deserialize)]
pub enum UninstallResponse {}

#[derive(Serialize, Deserialize)]
pub struct UpdateRequest;

#[derive(Serialize, Deserialize)]
pub enum UpdateResponse {}
