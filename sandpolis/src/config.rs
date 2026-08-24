use anyhow::{Context, Result, bail};
use fs2::FileExt;
use sandpolis_instance::realm::RealmName;
use sandpolis_instance::realm::config::{CaConfig, CertSource, RealmBootstrap, ron_options};
use sandpolis_instance::realm::url::ServerUrl;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs::OpenOptions;
use std::io::{Read, Seek, SeekFrom, Write};
use std::path::{Path, PathBuf};
use std::sync::{LazyLock, Mutex};
use tracing::debug;

/// The filename suffix of a realm config, whose stem is the realm's name.
pub const REALM_CONFIG_SUFFIX: &str = ".realm.ron";

/// The realm a config file's name declares it to be for.
///
/// This doubles as the test for whether a file in the data directory is a realm
/// config at all, so the suffix is checked in exactly one place.
pub(crate) fn realm_name(path: &Path) -> Result<&str> {
    path.file_name()
        .and_then(|name| name.to_str())
        .and_then(|name| name.strip_suffix(REALM_CONFIG_SUFFIX))
        .filter(|name| !name.is_empty())
        .ok_or_else(|| {
            anyhow::anyhow!(
                "{} is not named `<realm>{REALM_CONFIG_SUFFIX}`",
                path.display()
            )
        })
}

/// What the server last read from or wrote to a realm config, or the fact that
/// the file changed underneath it and must never be written again.
///
/// This is how manual edits are told apart from the server's own write-backs:
/// every read and write records the exact bytes involved, so any other bytes on
/// disk mean someone edited the file. Contents are compared raw rather than
/// re-serialized, since reading and writing don't share RON extension flags.
enum ConfigDisposition {
    /// The exact bytes last read from or written to the file at `path`.
    Synced {
        contents: String,
        // The path only matters to the server's config watcher.
        #[cfg_attr(not(feature = "server"), allow(dead_code))]
        path: PathBuf,
    },
    /// The file was edited outside the server; leave it alone until restart.
    ReadOnly,
}

static CONFIG_REGISTRY: LazyLock<Mutex<HashMap<RealmName, ConfigDisposition>>> =
    LazyLock::new(Default::default);

/// Remember the contents the server itself just read or wrote, so a later
/// change on disk is recognizable as a manual edit.
fn record_synced(name: &RealmName, path: &Path, contents: &str) {
    let mut registry = CONFIG_REGISTRY.lock().unwrap();
    if !matches!(registry.get(name), Some(ConfigDisposition::ReadOnly)) {
        registry.insert(
            name.clone(),
            ConfigDisposition::Synced {
                contents: contents.to_string(),
                path: path.to_path_buf(),
            },
        );
    }
}

/// Stop ever writing this realm's config again. Returns whether this call made
/// the transition, so callers can warn exactly once.
pub(crate) fn mark_read_only(name: &RealmName) -> bool {
    !matches!(
        CONFIG_REGISTRY
            .lock()
            .unwrap()
            .insert(name.clone(), ConfigDisposition::ReadOnly),
        Some(ConfigDisposition::ReadOnly)
    )
}

pub(crate) fn is_read_only(name: &RealmName) -> bool {
    matches!(
        CONFIG_REGISTRY.lock().unwrap().get(name),
        Some(ConfigDisposition::ReadOnly)
    )
}

/// The bytes the server last synced for this realm: `None` if the realm was
/// never loaded, `Some(None)` if it's read-only, `Some(Some(_))` otherwise.
pub(crate) fn synced_contents(name: &RealmName) -> Option<Option<String>> {
    CONFIG_REGISTRY
        .lock()
        .unwrap()
        .get(name)
        .map(|disposition| match disposition {
            ConfigDisposition::Synced { contents, .. } => Some(contents.clone()),
            ConfigDisposition::ReadOnly => None,
        })
}

/// Every synced realm whose config file lives in `dir`, so the watcher can
/// notice deletions without claiming realms loaded from elsewhere.
#[cfg(feature = "server")]
pub(crate) fn synced_realms_in(dir: &Path) -> Vec<RealmName> {
    CONFIG_REGISTRY
        .lock()
        .unwrap()
        .iter()
        .filter_map(|(name, disposition)| match disposition {
            ConfigDisposition::Synced { path, .. } if path.parent() == Some(dir) => {
                Some(name.clone())
            }
            _ => None,
        })
        .collect()
}

fn manual_edit_error(name: &RealmName) -> anyhow::Error {
    anyhow::anyhow!(
        "Realm config {name} was edited on disk while the server was running; \
         refusing to overwrite it — restart the server to apply the manual changes"
    )
}

/// Refuse to write when the on-disk contents no longer match what the server
/// last synced, which means someone edited the file manually. Must be called
/// with the file's advisory lock held, so the comparison can't race a write.
fn ensure_still_synced(name: &RealmName, on_disk: &str) -> Result<()> {
    match synced_contents(name) {
        // Never loaded, so there's nothing to compare against.
        None => Ok(()),
        Some(Some(expected)) if on_disk == expected => Ok(()),
        _ => {
            mark_read_only(name);
            Err(manual_edit_error(name))
        }
    }
}

/// A realm as declared in a realm config, which the global stratum server reads
/// at startup.
///
/// The file *is* the realm: its name comes from the filename, and realms are
/// never created any other way. Everything here is scoped to the one realm —
/// instance-wide settings (where to listen, where the database lives) are CLI
/// flags, because they describe this process rather than the estate.
#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
#[derive(Serialize, Deserialize, Debug, Clone, Default)]
#[serde(default)]
pub struct RealmConfig {
    /// Path this was loaded from, so write-back knows where to go.
    #[serde(skip)]
    path: Option<PathBuf>,

    /// The realm's name, taken from the filename rather than the contents so a
    /// file can't claim to be a realm it isn't.
    #[serde(skip)]
    pub name: RealmName,

    /// Address clients and agents use to reach this realm, as `host` or
    /// `host:port`. Certificates minted for the realm name it in their common
    /// name, so it must be how the server is actually reachable from outside.
    ///
    /// Absent means loopback, which is enough for instances started on the same
    /// host but not for anything else.
    pub address: Option<String>,

    /// The realm's root certificate authority.
    ///
    /// Absent — as in a blank file — means "generate one for me": the server
    /// mints a CA on first start and writes it back here inline, after which
    /// this file is the durable copy.
    pub ca: Option<CaConfig>,

    #[cfg(feature = "account")]
    pub account: sandpolis_account::config::AccountManagerConfig,

    #[cfg(feature = "inventory")]
    pub inventory: sandpolis_inventory::config::InventoryManagerConfig,

    #[cfg(feature = "probe")]
    pub probe: sandpolis_probe::config::ProbeManagerConfig,

    /// User accounts that clients (not agents) login to, and what each is
    /// allowed to do. Editing this section is the only way accounts are ever
    /// created, changed, or removed.
    pub user: sandpolis_server::user::config::UsersConfig,
}

impl RealmConfig {
    /// Load a realm config.
    ///
    /// A blank file (empty or whitespace) is the "generate everything for me"
    /// case and yields defaults. Malformed RON is an error rather than a blank
    /// file, so a typo never causes the CA to be silently regenerated over
    /// working configuration.
    pub fn load<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let path = path.as_ref();
        debug!(path = %path.display(), "Loading realm config");

        let name = realm_name(path)?.parse::<RealmName>().with_context(|| {
            format!(
                "{} is not a valid realm name (lowercase letters and digits, 4-32 characters)",
                path.display()
            )
        })?;

        let contents =
            std::fs::read_to_string(path).with_context(|| format!("Reading {}", path.display()))?;

        let mut config: Self = if contents.trim().is_empty() {
            Self::default()
        } else {
            ron_options()
                .from_str(&contents)
                .with_context(|| format!("Parsing {}", path.display()))?
        };

        config.path = Some(path.to_path_buf());
        config.name = name;
        record_synced(&config.name, path, &contents);
        Ok(config)
    }

    /// Every realm declared in `dir`, one per `*.realm.ron` file.
    ///
    /// This is how the global stratum server finds the realms it serves: the
    /// directory holding its database is also where its realm configs live, so
    /// nothing outside has to enumerate them. An empty directory gets a blank
    /// `default.realm.ron`, since a server that serves nothing is never what was
    /// wanted; its CA is minted on this same start and written back.
    pub fn load_dir<P>(dir: P) -> Result<Vec<Self>>
    where
        P: AsRef<Path>,
    {
        let dir = dir.as_ref();
        std::fs::create_dir_all(dir).with_context(|| format!("Creating {}", dir.display()))?;

        let entries: Vec<PathBuf> = std::fs::read_dir(dir)
            .with_context(|| format!("Reading {}", dir.display()))?
            .map(|entry| entry.map(|entry| entry.path()))
            .collect::<std::io::Result<Vec<_>>>()?;

        // TODO remove legacy detection
        let legacy: Vec<String> = entries
            .iter()
            .filter(|path| {
                path.extension()
                    .is_some_and(|ext| ext == "realm" || ext == "server")
            })
            .filter_map(|path| path.file_name()?.to_str().map(String::from))
            .collect();
        if !legacy.is_empty() {
            bail!(
                "{} holds files named the old way ({}). Realm configs are now \
                 `<realm>{REALM_CONFIG_SUFFIX}` and realm certs are \
                 `<realm>{}`; rename them and start again.",
                dir.display(),
                legacy.join(", "),
                sandpolis_instance::realm::config::REALM_CERT_SUFFIX,
            );
        }

        let mut paths: Vec<PathBuf> = entries
            .into_iter()
            .filter(|path| realm_name(path).is_ok())
            .collect();

        // Sorted so the set doesn't depend on directory iteration order, which
        // decides which realm's `account.scrape` section wins.
        paths.sort();

        if paths.is_empty() {
            let path = dir.join(format!("default{REALM_CONFIG_SUFFIX}"));
            debug!(path = %path.display(), "Creating the initial realm config");
            std::fs::write(&path, "").with_context(|| format!("Creating {}", path.display()))?;
            paths.push(path);
        }

        paths.into_iter().map(Self::load).collect()
    }

    /// The directory this was loaded from, which relative certificate paths
    /// resolve against.
    pub fn base_dir(&self) -> Option<&Path> {
        self.path.as_deref().and_then(|path| path.parent())
    }

    pub fn path(&self) -> Option<&Path> {
        self.path.as_deref()
    }

    /// The address certificates minted for this realm will name.
    pub fn server_url(&self) -> Result<Option<ServerUrl>> {
        let Some(address) = self.address.as_ref() else {
            return Ok(None);
        };
        let mut url: ServerUrl = address
            .parse()
            .with_context(|| format!("Parsing address {address:?}"))?;
        url.realm = self.name.clone();
        Ok(Some(url))
    }

    /// Turn this into what [`RealmManager::new`] needs to bring the realm up.
    ///
    /// [`RealmManager::new`]: sandpolis_instance::realm::RealmManager::new
    pub fn bootstrap(&self) -> Result<RealmBootstrap> {
        let ca = match self.ca.as_ref() {
            Some(ca) => {
                let (cert, key) = ca.load_der(self.base_dir())?;
                let key = key.ok_or_else(|| {
                    anyhow::anyhow!(
                        "Realm {} declares a CA certificate without its private key; \
                         the global stratum server must be able to issue from it",
                        self.name
                    )
                })?;
                Some((cert, key))
            }
            None => None,
        };

        Ok(RealmBootstrap {
            name: self.name.clone(),
            ca,
            address: self.server_url()?,
        })
    }

    /// Write the realm config back to where it was loaded from.
    ///
    /// Acquires an exclusive advisory lock for the duration of the write so
    /// concurrent `sandpolis` processes can't clobber each other. The lock is
    /// released when the file handle is dropped.
    pub fn save(&self) -> Result<()> {
        let Some(path) = &self.path else {
            return Ok(());
        };
        if is_read_only(&self.name) {
            return Err(manual_edit_error(&self.name));
        }

        debug!(path = %path.display(), "Saving realm config");

        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let contents = ron::ser::to_string_pretty(self, ron::ser::PrettyConfig::default())?;

        let mut file = OpenOptions::new()
            .create(true)
            .read(true)
            .write(true)
            .truncate(false)
            .open(path)?;

        FileExt::lock_exclusive(&file)?;

        let mut on_disk = String::new();
        file.read_to_string(&mut on_disk)?;
        ensure_still_synced(&self.name, &on_disk)?;

        file.set_len(0)?;
        file.seek(SeekFrom::Start(0))?;
        file.write_all(contents.as_bytes())?;
        file.sync_all()?;
        record_synced(&self.name, path, &contents);

        Ok(())
    }

    /// Read-modify-write the on-disk realm config under an exclusive lock.
    ///
    /// Re-reads the file from disk after acquiring the lock so the closure
    /// always sees the latest committed state, then writes the mutated value
    /// back before releasing the lock. Use this whenever multiple processes may
    /// be racing to update the same realm.
    pub fn modify<F>(&mut self, f: F) -> Result<()>
    where
        F: FnOnce(&mut RealmConfig) -> Result<()>,
    {
        let Some(path) = self.path.clone() else {
            bail!("Realm config has no associated file path");
        };
        let name = self.name.clone();
        if is_read_only(&name) {
            return Err(manual_edit_error(&name));
        }

        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let mut file = OpenOptions::new()
            .create(true)
            .read(true)
            .write(true)
            .truncate(false)
            .open(&path)?;

        FileExt::lock_exclusive(&file)?;

        let mut buf = String::new();
        file.read_to_string(&mut buf)?;
        ensure_still_synced(&name, &buf)?;
        if !buf.trim().is_empty() {
            *self = ron_options()
                .from_str(&buf)
                .with_context(|| format!("Parsing {}", path.display()))?;
        }
        // The path and name aren't serialized, so restore them after the reload.
        self.path = Some(path);
        self.name = name;

        f(self)?;

        let contents = ron::ser::to_string_pretty(self, ron::ser::PrettyConfig::default())?;
        file.set_len(0)?;
        file.seek(SeekFrom::Start(0))?;
        file.write_all(contents.as_bytes())?;
        file.sync_all()?;
        if let Some(path) = self.path.as_deref() {
            record_synced(&self.name, path, &contents);
        }

        Ok(())
    }

    /// Record a realm CA the server generated, so the file becomes the durable
    /// copy of it.
    pub fn store_ca(&mut self, cert_pem: String, key_pem: String) -> Result<()> {
        self.modify(|config| {
            config.ca = Some(CaConfig {
                cert: CertSource::Inline(cert_pem.clone()),
                key: Some(CertSource::Inline(key_pem.clone())),
            });
            Ok(())
        })
    }
}

#[cfg(test)]
mod test_realm_config {
    use super::*;

    /// A blank file means "generate everything for me", and the realm's name
    /// comes from the filename rather than the contents.
    #[test]
    fn blank_file_loads_as_defaults() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("prod.realm.ron");
        std::fs::write(&path, "  \n ")?;

        let config = RealmConfig::load(&path)?;
        assert_eq!(config.name, "prod".parse()?);
        assert!(config.ca.is_none());
        assert!(config.address.is_none());
        Ok(())
    }

    /// Malformed RON is an error, never a blank file — otherwise a typo would
    /// silently discard a working CA on the next write-back.
    #[test]
    fn malformed_file_is_an_error() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("prod.realm.ron");
        std::fs::write(&path, "(address: ")?;

        assert!(RealmConfig::load(&path).is_err());
        Ok(())
    }

    /// A filename that isn't a valid realm name is rejected up front rather
    /// than producing a realm nothing can address.
    #[test]
    fn invalid_filename_is_rejected() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("Prod!.realm.ron");
        std::fs::write(&path, "")?;

        assert!(RealmConfig::load(&path).is_err());
        Ok(())
    }

    /// A generated CA is written back inline and survives a reload, so the
    /// second run reuses it instead of minting another.
    #[test]
    fn stored_ca_round_trips() -> Result<()> {
        let dir = tempfile::tempdir()?;
        // Realm names are unique per test: the sync registry is process-global,
        // so two tests writing the same name would race each other.
        let path = dir.path().join("storedca.realm.ron");
        std::fs::write(&path, "")?;

        let mut config = RealmConfig::load(&path)?;
        config.store_ca(
            "-----BEGIN CERTIFICATE-----\nAAAA\n-----END CERTIFICATE-----\n".into(),
            "-----BEGIN PRIVATE KEY-----\nBBBB\n-----END PRIVATE KEY-----\n".into(),
        )?;

        let reloaded = RealmConfig::load(&path)?;
        assert_eq!(reloaded.name, "storedca".parse()?);
        let ca = reloaded.ca.expect("the generated CA is written back");
        assert!(matches!(ca.cert, CertSource::Inline(_)));
        assert!(ca.key.is_some());
        Ok(())
    }

    /// A data directory with no realm config gets one, so a fresh install comes
    /// up serving something.
    #[test]
    fn empty_dir_gets_a_default_realm() -> Result<()> {
        let dir = tempfile::tempdir()?;

        let realms = RealmConfig::load_dir(dir.path())?;
        assert_eq!(realms.len(), 1);
        assert_eq!(realms[0].name, "default".parse()?);
        assert!(dir.path().join("default.realm.ron").exists());

        // A second start finds the file rather than making another one.
        let realms = RealmConfig::load_dir(dir.path())?;
        assert_eq!(realms.len(), 1);
        assert_eq!(
            realms[0].path(),
            Some(dir.path().join("default.realm.ron")).as_deref()
        );
        Ok(())
    }

    /// Every realm config is loaded, in a fixed order, and nothing else in the
    /// directory is mistaken for one — the database and the realm certs live
    /// here too.
    #[test]
    fn every_realm_file_is_loaded_in_order() -> Result<()> {
        let dir = tempfile::tempdir()?;
        std::fs::write(dir.path().join("prod.realm.ron"), "")?;
        std::fs::write(dir.path().join("beta.realm.ron"), "")?;
        std::fs::write(dir.path().join("prod.realm.pem"), "")?;
        std::fs::write(dir.path().join("default.db"), "")?;
        std::fs::write(dir.path().join("notes.txt"), "")?;

        let realms = RealmConfig::load_dir(dir.path())?;
        let names: Vec<_> = realms.iter().map(|realm| realm.name.to_string()).collect();
        assert_eq!(names, vec!["beta", "prod"]);
        Ok(())
    }

    /// A directory holding files from before the rename would otherwise match
    /// nothing, and the server would mint a second CA over a realm that already
    /// has one.
    #[test]
    fn legacy_filenames_are_refused() -> Result<()> {
        let dir = tempfile::tempdir()?;
        std::fs::write(dir.path().join("prod.realm"), "")?;

        let error = RealmConfig::load_dir(dir.path())
            .expect_err("the old naming is not silently ignored")
            .to_string();
        assert!(error.contains("prod.realm"), "{error}");
        assert!(!dir.path().join("default.realm.ron").exists());
        Ok(())
    }

    /// The address is realm-qualified, since that's what a certificate's common
    /// name has to encode.
    #[test]
    fn address_carries_the_realm() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("prod.realm.ron");
        std::fs::write(&path, r#"(address: "gs.example.com:9000")"#)?;

        let url = RealmConfig::load(&path)?
            .server_url()?
            .expect("an address was declared");
        assert_eq!(url.canonical(), "gs.example.com:9000/prod");
        Ok(())
    }

    /// A file edited on disk after loading is never overwritten: the write is
    /// refused, the realm becomes read-only, and the manual edit survives.
    #[test]
    fn manual_edit_makes_config_read_only() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("edited.realm.ron");
        std::fs::write(&path, "")?;

        let mut config = RealmConfig::load(&path)?;
        let edit = r#"(address: "gs.example.com:9000")"#;
        std::fs::write(&path, edit)?;

        assert!(config.modify(|_| Ok(())).is_err());
        assert!(is_read_only(&"edited".parse()?));

        // Later writes are refused up front, without touching the file.
        assert!(
            config
                .store_ca("AAAA".into(), "BBBB".into())
                .is_err()
        );
        assert_eq!(std::fs::read_to_string(&path)?, edit);
        Ok(())
    }

    /// The server's own write-backs don't count as manual edits, so repeated
    /// writes keep working.
    #[test]
    fn own_writes_stay_writable() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("ownwrites.realm.ron");
        std::fs::write(&path, "")?;

        let mut config = RealmConfig::load(&path)?;
        config.store_ca("AAAA".into(), "BBBB".into())?;
        config.modify(|c| {
            c.address = Some("gs.example.com:9000".into());
            Ok(())
        })?;

        assert!(!is_read_only(&"ownwrites".parse()?));
        Ok(())
    }
}
