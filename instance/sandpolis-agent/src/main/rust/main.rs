//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

#[path = "../../gen/rust"]
pub mod core {

	#[path = "core.foundation"]
	pub mod foundation {
		pub mod platform;
	}

	#[path = "core.instance"]
	pub mod instance {
		pub mod auth;
		pub mod group;
		pub mod metatypes;
	}

	#[path = "core.net"]
	pub mod net {
		pub mod message;
		pub mod messages;
	}
}

pub mod connection;

use anyhow::{bail, Result};
use crate::core::instance::group::*;
use log::{debug, info, error};
use protobuf::Message;
use rust_embed::RustEmbed;
use std::collections::HashMap;
use std::net::TcpStream;
use std::{thread, time};
use predicates::{Predicate, prelude::*};
use std::io::{BufRead};

/// Contains embedded resources
#[derive(RustEmbed)]
#[folder = "src/main/resources/agent"]
struct BinaryAssets;

fn main() -> Result<()> {

	// Initialize logging
	env_logger::init();

	// Load build metadata
	if let Some(build_properties) = BinaryAssets::get("build.properties") {
		let properties: HashMap<String, String> = parse_from_slice(&build_properties).expect("Failed to parse properties file").into_iter().collect();

		// Output debug build info
		debug!("Build platform: {}", properties["build.platform"]);
		debug!("Build JVM version: {}", properties["build.java_version"]);

		info!("Starting Sandpolis agent v{}", properties["build.version"]);
	} else {
		error!("Failed to locate embedded build.properties resource");
		bail!("");
	}

	// Load agent configuration
	if let Some(config_bytes) = BinaryAssets::get("config.bin") {
		if let Ok(config) = AgentConfig::parse_from_bytes(&config_bytes) {

		} else {
			error!("The embedded configuration is invalid")
		}
	} else {
		debug!("Failed to locate embedded configuration")
	}

	// Prompt user
	info!("Preparing to configure agent");
	print!("Please enter the server's address [127.0.0.1]: ");

	let mut server_host = String::new();
	std::io::stdin().read_line(&mut server_host)?;

	if server_host == "" {
		server_host = "127.0.0.1".to_string();
	}

	// Attempt a connection
	info!("Attempting connection to server");
	if let Ok(connection) = connect(server_host.as_str(), 8768) {
		// TODO
	}

	if prompt_bool("Configure client certificate authentication?", false) {

	}

	if prompt_bool("Configure password authentication? ", false) {
		let password = prompt_string("Enter password: ", "", &predicate::function(|x: &String| x.len() >= 5_usize));
	}

	return Ok(())
}

fn prompt_bool(prompt: &str, default: bool) -> bool {

	let answer = prompt_string(prompt, match default {
		true => "y",
		false => "n",
	}, &predicate::in_iter(vec!["y".to_string(), "Y".to_string(), "n".to_string(), "N".to_string()]));

	return match answer.as_str() {
		"y" => true,
		"n" => false,
		_ => false,
	}	
}

fn prompt_string(prompt: &str, default: &str, validator: &dyn Predicate<String>) -> String {

	for line in std::io::stdin().lock().lines().map(|l| l.unwrap()) {
		if validator.eval(&line) {
			return line;
		}
	}
	return "".to_string();
}

fn connection_routine(config: &AgentConfig_LoopConfig) {

	debug!("Starting connection routine");

	let mut iteration: i32 = 0;
	while iteration < config.iteration_limit || config.iteration_limit == 0 {
		iteration += 1;
		if let Ok(connection) = connect("127.0.0.1", 8768) {

		}

		thread::sleep(time::Duration::from_millis(config.cooldown as u64));
	}
}

fn dispatch_routine(connection: &mut Connection) {

	debug!("Starting command dispatch routine");

	loop {
		// TODO
	}
}
