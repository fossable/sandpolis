//! Just enough of `~/.ssh/config` to fill in a deploy dialog.
//!
//! The operator already told OpenSSH how to reach their hosts, so the dialog
//! reads that file rather than asking again. Only the four keywords a
//! deployment needs are understood — `HostName`, `User`, `Port`, `IdentityFile`
//! — and only simple `Host` patterns; `Match`, `Include`, and the rest are
//! ignored, so this is a convenience for prefilling and never the authority on
//! anything.

use std::path::PathBuf;

/// What the config file says about one host.
#[derive(Clone, Debug, Default, PartialEq)]
pub struct HostConfig {
    pub hostname: Option<String>,
    pub user: Option<String>,
    pub port: Option<u16>,
    pub identity_file: Option<String>,
}

impl HostConfig {
    /// Take any value `other` has that this one is missing.
    ///
    /// OpenSSH's rule: the first value found wins, and later blocks (including
    /// `Host *`) only fill gaps.
    fn fill_from(&mut self, other: &HostConfig) {
        if self.hostname.is_none() {
            self.hostname = other.hostname.clone();
        }
        if self.user.is_none() {
            self.user = other.user.clone();
        }
        if self.port.is_none() {
            self.port = other.port;
        }
        if self.identity_file.is_none() {
            self.identity_file = other.identity_file.clone();
        }
    }
}

/// Look `alias` up in the user's `~/.ssh/config`.
pub fn lookup(alias: &str) -> HostConfig {
    match std::fs::read_to_string(config_path()) {
        Ok(contents) => lookup_in(&contents, alias),
        Err(_) => HostConfig::default(),
    }
}

/// Path of the user's SSH config file.
fn config_path() -> PathBuf {
    home().join(".ssh").join("config")
}

fn home() -> PathBuf {
    dirs::home_dir().unwrap_or_else(|| PathBuf::from("/"))
}

/// Resolve `alias` against the contents of a config file.
pub fn lookup_in(contents: &str, alias: &str) -> HostConfig {
    let mut resolved = HostConfig::default();
    let mut current: Option<(bool, HostConfig)> = None;

    // Blocks are applied in file order, each only filling what earlier ones
    // left unset.
    let apply = |block: Option<(bool, HostConfig)>, resolved: &mut HostConfig| {
        if let Some((matches, config)) = block
            && matches
        {
            resolved.fill_from(&config);
        }
    };

    for line in contents.lines() {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }

        // Keyword and argument are separated by whitespace or `=`.
        let (keyword, value) = match line.split_once(['=', ' ', '\t']) {
            Some((keyword, value)) => (keyword.trim(), value.trim_start_matches(['=', ' ', '\t'])),
            None => continue,
        };
        let value = value.trim();

        if keyword.eq_ignore_ascii_case("Host") {
            apply(current.take(), &mut resolved);
            let matches = block_matches(value, alias);
            current = Some((matches, HostConfig::default()));
            continue;
        }

        let Some((_, config)) = current.as_mut() else {
            continue;
        };
        match keyword.to_ascii_lowercase().as_str() {
            "hostname" => config.hostname = Some(value.to_string()),
            "user" => config.user = Some(value.to_string()),
            "port" => config.port = value.parse().ok(),
            // A host may list several; the first is the one we'd try first too.
            "identityfile" if config.identity_file.is_none() => {
                config.identity_file = Some(expand_tilde(value))
            }
            _ => {}
        }
    }
    apply(current.take(), &mut resolved);

    resolved
}

/// Whether a `Host` line's patterns select `alias`.
///
/// Patterns support `*` and `?`, and a negated one (`!host`) excludes the block
/// outright — same rules OpenSSH applies.
fn block_matches(patterns: &str, alias: &str) -> bool {
    let mut matched = false;
    for pattern in patterns.split_whitespace() {
        match pattern.strip_prefix('!') {
            Some(negated) if matches_glob(negated, alias) => return false,
            Some(_) => {}
            None => matched |= matches_glob(pattern, alias),
        }
    }
    matched
}

/// Glob match over the whole string with `*` (any run) and `?` (one character).
fn matches_glob(pattern: &str, value: &str) -> bool {
    let pattern: Vec<char> = pattern.chars().collect();
    let value: Vec<char> = value.chars().collect();

    // Classic two-pointer wildcard match: on a mismatch, backtrack to the last
    // `*` and let it swallow one more character.
    let (mut p, mut v) = (0, 0);
    let (mut star, mut backtrack) = (None, 0);
    while v < value.len() {
        if p < pattern.len() && (pattern[p] == '?' || pattern[p] == value[v]) {
            p += 1;
            v += 1;
        } else if p < pattern.len() && pattern[p] == '*' {
            star = Some(p);
            backtrack = v;
            p += 1;
        } else if let Some(star) = star {
            p = star + 1;
            backtrack += 1;
            v = backtrack;
        } else {
            return false;
        }
    }
    pattern[p..].iter().all(|c| *c == '*')
}

/// Expand a leading `~` to the user's home directory.
pub fn expand_tilde(path: &str) -> String {
    match path.strip_prefix("~/") {
        Some(rest) => home().join(rest).display().to_string(),
        None => path.to_string(),
    }
}

/// The identity file to offer when neither the dialog nor the config names one:
/// the first key that actually exists, in the order `ssh` itself prefers.
pub fn default_identity_file() -> Option<String> {
    let ssh = home().join(".ssh");
    ["id_ed25519", "id_ecdsa", "id_rsa"]
        .into_iter()
        .map(|name| ssh.join(name))
        .find(|path| path.is_file())
        .map(|path| path.display().to_string())
}

/// The account to offer when nothing else names one.
pub fn default_username() -> String {
    std::env::var("USER")
        .or_else(|_| std::env::var("LOGNAME"))
        .unwrap_or_else(|_| "root".to_string())
}

#[cfg(test)]
mod test {
    use super::*;

    const CONFIG: &str = r#"
        # A comment
        Host build
            HostName build.example.com
            User builder
            Port 2222
            IdentityFile /keys/build_ed25519

        Host *.internal
            User ops

        Host *
            IdentityFile /keys/fallback
    "#;

    #[test]
    fn alias_resolves_every_keyword() {
        let config = lookup_in(CONFIG, "build");
        assert_eq!(config.hostname.as_deref(), Some("build.example.com"));
        assert_eq!(config.user.as_deref(), Some("builder"));
        assert_eq!(config.port, Some(2222));
        assert_eq!(config.identity_file.as_deref(), Some("/keys/build_ed25519"));
    }

    #[test]
    fn wildcard_blocks_match() {
        let config = lookup_in(CONFIG, "db1.internal");
        assert_eq!(config.user.as_deref(), Some("ops"));
        // Falls through to `Host *` for the key.
        assert_eq!(config.identity_file.as_deref(), Some("/keys/fallback"));
    }

    /// The first block to set a keyword wins, exactly as `ssh` reads it.
    #[test]
    fn earlier_blocks_win() {
        let config = lookup_in(CONFIG, "build");
        assert_eq!(config.identity_file.as_deref(), Some("/keys/build_ed25519"));
    }

    #[test]
    fn unknown_host_gets_only_the_catch_all() {
        let config = lookup_in(CONFIG, "nowhere");
        assert_eq!(config.hostname, None);
        assert_eq!(config.user, None);
        assert_eq!(config.identity_file.as_deref(), Some("/keys/fallback"));
    }

    #[test]
    fn keyword_equals_value_is_accepted() {
        let config = lookup_in("Host=build\nUser=builder\n", "build");
        assert_eq!(config.user.as_deref(), Some("builder"));
    }
}
