//! Linux display-server helpers used by the capture and input modules.

use anyhow::Result;
use std::sync::LazyLock;

use sctk::{
    output::OutputData,
    output::{OutputHandler, OutputState},
    reexports::client::protocol::wl_output::WlOutput,
    reexports::client::{Connection, Proxy, QueueHandle, globals},
    registry::{ProvidesRegistryState, RegistryState},
};

/// Path to `sh`, resolved once so callers work even with a minimal PATH.
pub static CMD_SH: LazyLock<String> = LazyLock::new(|| find_cmd_path("sh"));

fn find_cmd_path(cmd: &str) -> String {
    let dirs = std::env::var("PATH").unwrap_or_default();
    for dir in dirs.split(':').chain(["/usr/bin", "/bin"]) {
        let path = std::path::Path::new(dir).join(cmd);
        if path.is_file() {
            return path.to_string_lossy().into_owned();
        }
    }
    cmd.to_string()
}

/// Whether the current session is a Wayland desktop session.
///
/// The agent runs inside the user's session, so the session environment is
/// authoritative; no loginctl/seat probing is needed.
pub fn is_desktop_wayland() -> bool {
    match std::env::var("XDG_SESSION_TYPE").as_deref() {
        Ok("wayland") => true,
        Ok("x11") | Ok("tty") => false,
        _ => {
            // Fall back to display-related variables.
            std::env::var("WAYLAND_DISPLAY").is_ok_and(|v| !v.is_empty())
                && std::env::var("DISPLAY").map_or(true, |v| v.is_empty())
        }
    }
}

#[inline]
pub fn is_x11_or_headless() -> bool {
    !is_desktop_wayland()
}

/// Whether a KDE session is running (KDE's portal needs special casing).
pub fn is_kde_session() -> bool {
    std::process::Command::new(CMD_SH.as_str())
        .arg("-c")
        .arg("pgrep -f kded[0-9]+")
        .stdout(std::process::Stdio::piped())
        .output()
        .map(|o| !o.stdout.is_empty())
        .unwrap_or(false)
}

#[derive(Debug, Clone)]
pub struct WaylandDisplayInfo {
    pub name: String,
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
    pub logical_size: Option<(i32, i32)>,
    pub refresh_rate: i32,
}

// Retrieves information about all connected displays via the Wayland protocol.
pub fn get_wayland_displays() -> Result<Vec<WaylandDisplayInfo>> {
    struct WaylandEnv {
        registry_state: RegistryState,
        output_state: OutputState,
    }

    impl OutputHandler for WaylandEnv {
        fn output_state(&mut self) -> &mut OutputState {
            &mut self.output_state
        }

        fn new_output(&mut self, _: &Connection, _: &QueueHandle<Self>, _: WlOutput) {}
        fn update_output(&mut self, _: &Connection, _: &QueueHandle<Self>, _: WlOutput) {}
        fn output_destroyed(&mut self, _: &Connection, _: &QueueHandle<Self>, _: WlOutput) {}
    }

    impl ProvidesRegistryState for WaylandEnv {
        fn registry(&mut self) -> &mut RegistryState {
            &mut self.registry_state
        }

        sctk::registry_handlers!();
    }

    sctk::delegate_output!(WaylandEnv);
    sctk::delegate_registry!(WaylandEnv);

    let conn = Connection::connect_to_env()?;
    let (globals, mut event_queue) = globals::registry_queue_init(&conn)?;
    let queue_handle = event_queue.handle();

    let registry_state = RegistryState::new(&globals);
    let output_state = OutputState::new(&globals, &queue_handle);

    let mut environment = WaylandEnv {
        registry_state,
        output_state,
    };

    event_queue.roundtrip(&mut environment)?;

    let outputs: Vec<_> = environment.output_state.outputs().collect();
    let mut display_infos = Vec::new();

    for output in outputs {
        if let Some(output_data) = output.data::<OutputData>() {
            output_data.with_output_info(|info| {
                if let Some(mode) = info.modes.iter().find(|m| m.current) {
                    let (x, y) = info.location;
                    let (width, height) = mode.dimensions;
                    let refresh_rate = mode.refresh_rate;
                    let name = info.name.clone().unwrap_or_default();
                    let logical_size = info.logical_size;
                    display_infos.push(WaylandDisplayInfo {
                        name,
                        x,
                        y,
                        width,
                        height,
                        logical_size,
                        refresh_rate,
                    });
                }
            });
        }
    }

    Ok(display_infos)
}
