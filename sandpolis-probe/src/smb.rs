//! SMB probe support.
//!
//! SMB devices are modelled end to end — [`SmbProbeConfig`](crate::config::SmbProbeConfig),
//! [`ProbeType::Smb`](crate::ProbeType::Smb), a node icon, a panel tab, and
//! visibility in the filesystem subsystem — but there is no backend behind
//! [`crate::filesystem`] yet, because no usable SMB client crate can be linked
//! into this workspace today:
//!
//! - `smb` (the pure-Rust client) pins `sspi =0.18.7`, which pins
//!   `crypto-bigint =0.7.0-rc.8`. `russh` requires `crypto-bigint ^0.7.3`, and
//!   cargo can only resolve one `0.7.x`. Moving `smb` onto `sspi 0.21` (which
//!   relaxes the pin to `^0.7`) cascades into its other RustCrypto release-candidate
//!   pins — `cmac 0.8.0-rc.1` and `kbkdf` stop compiling against the newer
//!   `digest`/`crypto-common` generation — so it would mean forking and
//!   maintaining that crate's whole crypto stack.
//! - `pavao` avoids the conflict entirely by binding to Samba's `libsmbclient`,
//!   at the cost of a native dependency in `shell.nix` and the server image.
//!
//! When one of those is resolved, implement `SmbFs` here with the same methods
//! [`NfsFs`](crate::nfs::NfsFs) exposes and add the `ProbeFs::Smb` arm; nothing
//! outside this module and `filesystem.rs` needs to change.

/// Why SMB operations decline, surfaced to the client as the failure reason.
#[cfg(feature = "server")]
pub(crate) const UNSUPPORTED: &str =
    "SMB probes are not yet supported: no SMB client crate is compatible with this build";
