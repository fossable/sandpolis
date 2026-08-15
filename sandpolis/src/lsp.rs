use anyhow::{Result, bail};
use roniker::RustAnalyzer;
use tracing::debug;

/// Which file format the LSP serves.
///
/// Realm configs are the only RON documents Sandpolis has, but the flag is
/// still required: it picks the root type the document's top level is resolved
/// against, and every completion and diagnostic follows from that.
#[derive(clap::Args, Debug, Clone)]
pub struct LspArgs {
    /// Serve realm configs (`*.realm.ron`)
    #[clap(long, required = true)]
    pub realm: bool,
}

impl LspArgs {
    /// Crate-qualified path of the type a document deserializes as, named the
    /// way `build.rs` indexed it.
    pub fn root_type(&self) -> &'static str {
        "crate::config::RealmConfig"
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

    /// The root type has to be in the index `build.rs` produced, or the LSP
    /// would come up serving nothing.
    #[test]
    fn the_root_type_is_indexed() {
        let analyzer: RustAnalyzer = serde_json::from_str(include_str!(concat!(
            env!("OUT_DIR"),
            "/rust_analyzer.json"
        )))
        .expect("the generated index deserializes");

        let root_type = LspArgs { realm: true }.root_type();
        assert!(
            analyzer.has_type(root_type),
            "{root_type} is missing from the generated index"
        );
    }
}
