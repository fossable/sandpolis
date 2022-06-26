//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

use std::io::BufRead;

use log::debug;
use std::fs::File;
use std::io;
use std::io::BufReader;
use std::path::Path;
use std::process::Command;

pub enum Type {
    Simple,
    Exec,
    Forking,
    Oneshot,
    Dbus,
    Notify,
    Idle,
}

pub enum Restart {
    No,
    Always,
    OnSuccess,
    OnFailure,
    OnAbnormal,
    OnAbort,
    OnWatchdog,
}

pub enum OOMPolicy {
    Continue,
    Stop,
    Kill,
}

pub struct SystemdService {
    pub r#type: Option<Type>,
    pub remain_after_exit: Option<bool>,
    pub guess_main_pid: Option<bool>,
    pub pid_file: Option<Box<Path>>,
    pub bus_name: Option<String>,
    pub exec_start: Vec<String>,
    pub exec_start_pre: Vec<String>,
    pub exec_start_post: Vec<String>,
    pub exec_condition: Vec<String>,
    pub exec_reload: Vec<String>,
    pub exec_stop: Vec<String>,
    pub exec_stop_post: Vec<String>,
    pub restart_sec: Option<u32>,
    pub timeout_start_sec: Option<u32>,
    pub timeout_stop_sec: Option<u32>,
    pub timeout_abort_sec: Option<u32>,
    pub timeout_sec: Option<u32>,
    pub runtime_max_sec: Option<u32>,
    pub watchdog_sec: Option<u32>,
    pub restart: Option<Restart>,
    pub success_exit_status: Vec<u32>,
    pub restart_prevent_exit_status: Vec<u32>,
    pub restart_force_exit_status: Vec<u32>,
    pub root_directory_start_only: Option<bool>,
    pub non_blocking: Option<bool>,
    pub file_descriptor_store_max: Option<u32>,
    pub oom_policy: Option<OOMPolicy>,
}

fn parse_bool(value: &str) -> bool {
    match value {
        "yes" => true,
        _ => false,
    }
}

impl SystemdService {
    pub fn blank() -> SystemdService {
        SystemdService {
            r#type: None,
            remain_after_exit: None,
            guess_main_pid: None,
            pid_file: None,
            bus_name: None,
            exec_start: vec![],
            exec_start_pre: vec![],
            exec_start_post: vec![],
            exec_condition: vec![],
            exec_reload: vec![],
            exec_stop: vec![],
            exec_stop_post: vec![],
            restart_sec: None,
            timeout_start_sec: None,
            timeout_stop_sec: None,
            timeout_abort_sec: None,
            timeout_sec: None,
            runtime_max_sec: None,
            watchdog_sec: None,
            restart: None,
            success_exit_status: vec![],
            restart_prevent_exit_status: vec![],
            restart_force_exit_status: vec![],
            root_directory_start_only: None,
            non_blocking: None,
            file_descriptor_store_max: None,
            oom_policy: None,
        }
    }

    pub fn new<P>(path: P) -> io::Result<SystemdService>
    where
        P: AsRef<Path>,
    {
        let file = File::open(path)?;

        let mut service = SystemdService::blank();

        for l in BufReader::new(file).lines() {
            let line = l?;

            if let Some(i) = line.find('=') {
                let key = &line[0..i];
                let value = &line[i..];

                match key {
                    "remain_after_exit" => {
                        service.remain_after_exit = Some(parse_bool(&value));
                    }
                    "guess_main_pid" => {
                        service.guess_main_pid = Some(parse_bool(&value));
                    }
                    "root_directory_start_only" => {
                        service.root_directory_start_only = Some(parse_bool(&value));
                    }
                    "non_blocking" => {
                        service.non_blocking = Some(parse_bool(&value));
                    }
                    _ => {
                        debug!("Unknown key: {}", key);
                    }
                }
            }
        }

        Ok(service)
    }

    pub fn install(&self) {}

    pub fn stop(&self) {
        if let Ok(status) = Command::new("systemctl").arg("stop").arg("name").status() {
            if status.success() {
                debug!("Successfully stopped service");
            }
        }
    }
}
