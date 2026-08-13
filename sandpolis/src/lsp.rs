use anyhow::{Result, bail};
use roniker::RustAnalyzer;
use tracing::debug;

/// Which file format the LSP serves.
///
/// Both formats are RON, so nothing about a document's contents says which one
/// it is — the editor has to say. This picks the root type the document's top
/// level is resolved against, and every completion and diagnostic follows from
/// that.
#[derive(clap::Args, Debug, Clone)]
#[group(required = true, multiple = false)]
pub struct LspArgs {
    /// Serve `.realm` files
    #[clap(long)]
    pub realm: bool,

    /// Serve `.server` files
    #[clap(long)]
    pub server: bool,
}

impl LspArgs {
    /// Crate-qualified path of the type a document deserializes as, named the
    /// way `build.rs` indexed it.
    pub fn root_type(&self) -> &'static str {
        if self.server {
            "sandpolis_instance::realm::config::ServerCertFile"
        } else {
            "crate::config::RealmConfig"
        }
    }
}

pub async fn run(args: LspArgs) -> Result<()> {
    let mut rust_analyzer: RustAnalyzer = serde_json::from_str(include_str!(concat!(
        env!("OUT_DIR"),
        "/rust_analyzer.json"
    )))
    .expect("Failed to deserialize RustAnalyzer");

    // The analyzer indexes every config type in the workspace; which one is the
    // document root is decided here rather than at build time.
    let root_type = args.root_type();
    if !rust_analyzer.has_type(root_type) {
        bail!(
            "{root_type} is missing from the analyzer index, so no completions \
             could be served for it"
        );
    }
    rust_analyzer.root_type = Some(root_type.to_string());

    debug!(
        types = rust_analyzer.type_count(),
        root = root_type,
        "Starting LSP server"
    );

    roniker::serve(rust_analyzer, false).await;
    Ok(())
}

#[cfg(test)]
mod test_lsp_args {
    use super::*;

    /// Each flag selects the root type for the file format it names.
    #[test]
    fn flags_select_the_root_type() {
        assert_eq!(
            LspArgs {
                realm: true,
                server: false
            }
            .root_type(),
            "crate::config::RealmConfig"
        );
        assert_eq!(
            LspArgs {
                realm: false,
                server: true
            }
            .root_type(),
            "sandpolis_instance::realm::config::ServerCertFile"
        );
    }

    /// Both root types have to be in the index `build.rs` produced, or the LSP
    /// would come up serving nothing.
    #[test]
    fn both_root_types_are_indexed() {
        let analyzer: RustAnalyzer = serde_json::from_str(include_str!(concat!(
            env!("OUT_DIR"),
            "/rust_analyzer.json"
        )))
        .expect("the generated index deserializes");

        for root_type in [
            "crate::config::RealmConfig",
            "sandpolis_instance::realm::config::ServerCertFile",
        ] {
            assert!(
                analyzer.has_type(root_type),
                "{root_type} is missing from the generated index"
            );
        }
    }
}
