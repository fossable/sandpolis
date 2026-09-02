//! Resolving an operator's deploy request into concrete connection details.
//!
//! The GUI dialog and the `agents deploy` CLI collect the same handful of
//! fields, and anything left blank is filled in the same way: explicit value,
//! then what `~/.ssh/config` says about the host, then the operator's
//! environment. This module is that shared resolution, kept free of any UI so
//! both front-ends drive it.

use super::ssh_config::{self, HostConfig};
use crate::deploy::DeployAuth;
use anyhow::{Context, Result, bail};

/// What each field falls back to when the operator leaves it blank. Read from
/// the operator's environment up front, so a form can say what will actually
/// be used.
#[derive(Clone, Debug, Default)]
pub struct DeployDefaults {
    pub username: String,
    pub identity_file: Option<String>,
}

impl DeployDefaults {
    /// Read the defaults from the operator's environment.
    pub fn detect() -> Self {
        Self {
            username: ssh_config::default_username(),
            identity_file: ssh_config::default_identity_file(),
        }
    }
}

/// The operator's (possibly blank) fields before resolution fills the gaps.
#[derive(Clone, Debug, Default)]
pub struct DeployForm {
    /// Host or `~/.ssh/config` alias. Required.
    pub host: String,
    pub username: String,
    pub port: Option<u16>,
    pub key_path: String,
    /// Whether a password was supplied up front. A typed password wins over a
    /// *default* key the operator never mentioned (not over an explicit or
    /// configured one), so resolution needs to know one exists — but not the
    /// secret itself.
    pub have_password: bool,
    /// Expected SHA256 host key fingerprint; `None` trusts on first use.
    pub fingerprint: Option<String>,
}

/// Connection details after resolution, before any secret is read.
#[derive(Clone, Debug)]
pub struct ResolvedTarget {
    pub host: String,
    pub port: u16,
    pub username: String,
    /// The private key that will authenticate, when one resolved.
    pub key_path: Option<String>,
    pub fingerprint: Option<String>,
}

/// Resolve `form` against one host's config: explicit field, then what ssh
/// would use for this host, then the default.
pub fn resolve(
    form: &DeployForm,
    configured: &HostConfig,
    defaults: &DeployDefaults,
) -> Result<ResolvedTarget> {
    let alias = form.host.trim();
    if alias.is_empty() {
        bail!("A host is required");
    }

    let host = configured.hostname.clone().unwrap_or_else(|| alias.into());
    let username = first_non_empty([
        form.username.trim().to_string(),
        configured.user.clone().unwrap_or_default(),
        defaults.username.clone(),
    ]);
    let port = form.port.or(configured.port).unwrap_or(22);

    let key_path = first_non_empty([
        ssh_config::expand_tilde(form.key_path.trim()),
        configured.identity_file.clone().unwrap_or_default(),
        // A password the operator typed wins over a default key they never
        // mentioned; without one, fall back to their usual key.
        if form.have_password {
            String::new()
        } else {
            defaults.identity_file.clone().unwrap_or_default()
        },
    ]);

    Ok(ResolvedTarget {
        host,
        port,
        username,
        key_path: (!key_path.is_empty()).then_some(key_path),
        fingerprint: form.fingerprint.clone(),
    })
}

/// [`resolve`] against the operator's actual `~/.ssh/config`.
pub fn resolve_with_ssh_config(form: &DeployForm, defaults: &DeployDefaults) -> Result<ResolvedTarget> {
    resolve(form, &ssh_config::lookup(form.host.trim()), defaults)
}

/// Read the resolved key (if any) and build the auth payload. `password` is
/// the account password when no key resolved, otherwise the key's passphrase.
pub fn read_auth(key_path: Option<&str>, password: &str) -> Result<DeployAuth> {
    match key_path {
        Some(key_path) => {
            // Read here rather than on the server: the key is on this machine,
            // and the server is the one that has to authenticate with it.
            let pem = std::fs::read_to_string(key_path)
                .with_context(|| format!("reading the private key at {key_path}"))?;
            Ok(DeployAuth::PrivateKey {
                pem,
                passphrase: (!password.is_empty()).then(|| password.to_string()),
            })
        }
        None if !password.is_empty() => Ok(DeployAuth::Password(password.to_string())),
        None => bail!("No private key or password to authenticate with"),
    }
}

/// Whether the key at rest needs a passphrase, as far as its PEM armor says.
/// OpenSSH-format keys hide their KDF in the base64, so an encrypted one slips
/// through here and fails server-side with a decode error instead.
pub fn key_looks_encrypted(pem: &str) -> bool {
    pem.contains("ENCRYPTED")
}

/// The first entry that isn't blank.
fn first_non_empty<const N: usize>(candidates: [String; N]) -> String {
    candidates
        .into_iter()
        .find(|candidate| !candidate.is_empty())
        .unwrap_or_default()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn defaults() -> DeployDefaults {
        DeployDefaults {
            username: "envuser".to_string(),
            identity_file: Some("/home/envuser/.ssh/id_ed25519".to_string()),
        }
    }

    fn form(host: &str) -> DeployForm {
        DeployForm {
            host: host.to_string(),
            ..Default::default()
        }
    }

    #[test]
    fn explicit_fields_beat_config_and_defaults() {
        let configured = HostConfig {
            hostname: Some("configured.example".to_string()),
            user: Some("configuser".to_string()),
            port: Some(2200),
            identity_file: Some("/config/key".to_string()),
        };
        let resolved = resolve(
            &DeployForm {
                host: "web".to_string(),
                username: "explicit".to_string(),
                port: Some(2222),
                key_path: "/explicit/key".to_string(),
                ..Default::default()
            },
            &configured,
            &defaults(),
        )
        .unwrap();

        assert_eq!(resolved.host, "configured.example");
        assert_eq!(resolved.username, "explicit");
        assert_eq!(resolved.port, 2222);
        assert_eq!(resolved.key_path.as_deref(), Some("/explicit/key"));
    }

    #[test]
    fn config_beats_defaults() {
        let configured = HostConfig {
            hostname: None,
            user: Some("configuser".to_string()),
            port: Some(2200),
            identity_file: Some("/config/key".to_string()),
        };
        let resolved = resolve(&form("web"), &configured, &defaults()).unwrap();

        assert_eq!(resolved.host, "web");
        assert_eq!(resolved.username, "configuser");
        assert_eq!(resolved.port, 2200);
        assert_eq!(resolved.key_path.as_deref(), Some("/config/key"));
    }

    #[test]
    fn defaults_fill_an_empty_config() {
        let resolved = resolve(&form("web"), &HostConfig::default(), &defaults()).unwrap();

        assert_eq!(resolved.host, "web");
        assert_eq!(resolved.username, "envuser");
        assert_eq!(resolved.port, 22);
        assert_eq!(
            resolved.key_path.as_deref(),
            Some("/home/envuser/.ssh/id_ed25519")
        );
    }

    #[test]
    fn password_suppresses_only_the_default_key() {
        let mut with_password = form("web");
        with_password.have_password = true;
        let resolved = resolve(&with_password, &HostConfig::default(), &defaults()).unwrap();
        assert_eq!(resolved.key_path, None);

        // A key the config names is one the operator chose; it stays.
        let configured = HostConfig {
            identity_file: Some("/config/key".to_string()),
            ..Default::default()
        };
        let resolved = resolve(&with_password, &configured, &defaults()).unwrap();
        assert_eq!(resolved.key_path.as_deref(), Some("/config/key"));
    }

    #[test]
    fn blank_host_is_rejected() {
        assert!(resolve(&form("  "), &HostConfig::default(), &defaults()).is_err());
    }

    #[test]
    fn read_auth_requires_a_key_or_password() {
        assert!(read_auth(None, "").is_err());
        assert!(matches!(
            read_auth(None, "hunter2").unwrap(),
            DeployAuth::Password(_)
        ));
    }

    #[test]
    fn read_auth_reads_the_key_file() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("key.pem");
        std::fs::write(&path, "fake key material").unwrap();
        let path = path.to_str().unwrap();

        match read_auth(Some(path), "").unwrap() {
            DeployAuth::PrivateKey { pem, passphrase } => {
                assert_eq!(pem, "fake key material");
                assert_eq!(passphrase, None);
            }
            _ => panic!("expected a private key"),
        }

        match read_auth(Some(path), "secret").unwrap() {
            DeployAuth::PrivateKey { passphrase, .. } => {
                assert_eq!(passphrase.as_deref(), Some("secret"));
            }
            _ => panic!("expected a private key"),
        }
    }
}
