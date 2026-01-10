use anyhow::{Result, anyhow};
use base64::{Engine as _, engine::general_purpose};
use std::collections::{HashMap, HashSet};
use std::path::{Path, PathBuf};
use std::process::Command;

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ShellCapability {
    Bash,
    Cmd,
    Pwsh,
    Sh,
    Zsh,
}

#[derive(Debug, Clone)]
pub struct Shell {
    pub executable: PathBuf,
    pub capabilities: HashSet<ShellCapability>,
    pub version: Option<String>,
}

impl Shell {
    fn known_paths() -> HashMap<&'static str, HashSet<ShellCapability>> {
        let mut paths = HashMap::new();

        // Bash
        paths.insert("/bin/bash", [ShellCapability::Bash].into());

        // Windows CMD
        paths.insert("C:/Windows/System32/cmd.exe", [ShellCapability::Cmd].into());

        // PowerShell
        paths.insert("/usr/bin/pwsh", [ShellCapability::Pwsh].into());
        paths.insert(
            "C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe",
            [ShellCapability::Pwsh].into(),
        );
        paths.insert(
            "C:/Windows/SysWOW64/WindowsPowerShell/v1.0/powershell.exe",
            [ShellCapability::Pwsh].into(),
        );

        // ZSH
        paths.insert("/usr/bin/zsh", [ShellCapability::Zsh].into());

        paths
    }

    pub fn discover_shells() -> Vec<Shell> {
        Self::known_paths()
            .keys()
            .filter_map(|path| Self::of_path(PathBuf::from(path)).ok())
            .collect()
    }

    pub fn of(executable: &str) -> Result<Shell> {
        Self::of_path(PathBuf::from(executable))
    }

    pub fn of_path(executable: PathBuf) -> Result<Shell> {
        if !executable.exists() || !Self::is_executable(&executable) {
            return Err(anyhow!("File is not executable: {:?}", executable));
        }

        let mut capabilities = HashSet::new();
        let mut version = None;

        // Check version output
        if let Ok(output) = Command::new(&executable).arg("--version").output()
            && output.status.success() {
                let stdout = String::from_utf8_lossy(&output.stdout);

                if stdout.starts_with("zsh") {
                    capabilities.insert(ShellCapability::Zsh);
                    capabilities.insert(ShellCapability::Bash);
                    capabilities.insert(ShellCapability::Sh);
                }

                if stdout.starts_with("GNU bash,") {
                    capabilities.insert(ShellCapability::Bash);
                    capabilities.insert(ShellCapability::Sh);
                }

                // Extract version if possible
                version = Some(stdout.lines().next().unwrap_or("").to_string());
            }

        // Check help output if we need more information
        if capabilities.is_empty()
            && let Ok(output) = Command::new(&executable).arg("--help").output()
                && output.status.success() {
                    let stdout = String::from_utf8_lossy(&output.stdout);

                    if stdout.starts_with("Usage: zsh") {
                        capabilities.insert(ShellCapability::Zsh);
                        capabilities.insert(ShellCapability::Bash);
                        capabilities.insert(ShellCapability::Sh);
                    }

                    if stdout.starts_with("GNU bash,") {
                        capabilities.insert(ShellCapability::Bash);
                        capabilities.insert(ShellCapability::Sh);
                    }
                }

        // If still no capabilities found, try to infer from known paths
        if capabilities.is_empty()
            && let Some(known_caps) = Self::known_paths().get(executable.to_str().unwrap_or("")) {
                capabilities = known_caps.clone();
            }

        Ok(Shell {
            executable,
            capabilities,
            version,
        })
    }

    pub fn execute(&self, command: &str) -> Result<Command> {
        let mut cmd = Command::new(&self.executable);

        if self.capabilities.contains(&ShellCapability::Sh) {
            // Use base64 encoding for POSIX shells to handle special characters
            let encoded_command = general_purpose::STANDARD.encode(command.as_bytes());
            cmd.arg("-c").arg(format!(
                "echo {} | base64 --decode | {}",
                encoded_command,
                self.executable.display()
            ));
        } else if self.capabilities.contains(&ShellCapability::Cmd) {
            cmd.arg("/C").arg(command);
        } else if self.capabilities.contains(&ShellCapability::Pwsh) {
            // PowerShell expects UTF-16LE for encoded commands
            let utf16_bytes: Vec<u8> = command
                .encode_utf16()
                .flat_map(|u| u.to_le_bytes().to_vec())
                .collect();
            let encoded_command = general_purpose::STANDARD.encode(&utf16_bytes);
            cmd.arg("-encodedCommand").arg(encoded_command);
        } else {
            return Err(anyhow!("Unsupported shell capabilities"));
        }

        Ok(cmd)
    }

    pub fn new_session(&self) -> Command {
        let mut cmd = Command::new(&self.executable);

        if self.capabilities.contains(&ShellCapability::Sh) {
            cmd.arg("-i");
        }

        cmd
    }

    fn is_executable(path: &Path) -> bool {
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            if let Ok(metadata) = path.metadata() {
                let permissions = metadata.permissions();
                permissions.mode() & 0o111 != 0
            } else {
                false
            }
        }

        #[cfg(windows)]
        {
            // On Windows, check if it's a .exe, .cmd, .bat file or if it exists
            path.extension()
                .and_then(|ext| ext.to_str())
                .map(|ext| matches!(ext.to_lowercase().as_str(), "exe" | "cmd" | "bat" | "ps1"))
                .unwrap_or(false)
                || path.exists()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_known_paths() {
        let paths = Shell::known_paths();
        assert!(!paths.is_empty());
        assert!(paths.contains_key("/bin/bash"));
    }

    #[test]
    fn test_shell_creation_with_nonexistent_path() {
        let result = Shell::of("/nonexistent/path");
        assert!(result.is_err());
    }
}
