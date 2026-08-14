//! Classifying a deploy target's operating system.
//!
//! The target is a remote host reached over SSH, so nothing here inspects the
//! local machine — everything is parsed out of what `cat /etc/os-release` and
//! `uname -m` printed on the other end.

use serde::{Deserialize, Serialize};

/// A deploy target's operating system, as far as deployment cares.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq)]
pub enum TargetOs {
    /// NixOS, which deployment refuses: its store is built from a
    /// configuration, so an agent dropped into `/opt` would be invisible to the
    /// system that is supposed to own it.
    NixOs,
    /// Some other Linux, named by its `os-release` `ID` (e.g. `debian`).
    Linux(String),
    /// Something we couldn't identify.
    Unknown,
}

impl std::fmt::Display for TargetOs {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NixOs => f.write_str("NixOS"),
            Self::Linux(id) => f.write_str(id),
            Self::Unknown => f.write_str("unknown"),
        }
    }
}

/// Classify a target from the contents of its `/etc/os-release`.
///
/// `ID_LIKE` is consulted as well so a NixOS derivative is refused along with
/// NixOS itself.
pub fn parse_os_release(contents: &str) -> TargetOs {
    let mut id = None;
    let mut id_like = None;

    for line in contents.lines() {
        let line = line.trim();
        let Some((key, value)) = line.split_once('=') else {
            continue;
        };
        // Values are shell-quoted, which for our purposes means the quotes are
        // decoration to be dropped.
        let value = value.trim().trim_matches(['"', '\'']).to_lowercase();
        match key.trim() {
            "ID" => id = Some(value),
            "ID_LIKE" => id_like = Some(value),
            _ => {}
        }
    }

    let is_nixos = id.as_deref() == Some("nixos")
        || id_like
            .as_deref()
            .is_some_and(|likes| likes.split_whitespace().any(|like| like == "nixos"));
    if is_nixos {
        return TargetOs::NixOs;
    }

    match id {
        Some(id) => TargetOs::Linux(id),
        None => TargetOs::Unknown,
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn nixos_is_recognized() {
        let release = r#"
            NAME=NixOS
            ID=nixos
            VERSION="25.05 (Warbler)"
        "#;
        assert_eq!(parse_os_release(release), TargetOs::NixOs);
    }

    #[test]
    fn nixos_derivative_is_recognized() {
        let release = "ID=someos\nID_LIKE=\"nixos\"\n";
        assert_eq!(parse_os_release(release), TargetOs::NixOs);
    }

    #[test]
    fn ordinary_distro_keeps_its_id() {
        let release = "NAME=\"Debian GNU/Linux\"\nID=debian\n";
        assert_eq!(parse_os_release(release), TargetOs::Linux("debian".into()));
    }

    /// A host with no `/etc/os-release` at all leaves us with an empty string.
    #[test]
    fn missing_release_is_unknown() {
        assert_eq!(parse_os_release(""), TargetOs::Unknown);
    }
}
