use roniker::RustAnalyzer;
use tracing::info;

pub async fn run() {
    let rust_analyzer: RustAnalyzer = serde_json::from_str(include_str!(concat!(
        env!("OUT_DIR"),
        "/rust_analyzer.json"
    )))
    .expect("Failed to deserialize RustAnalyzer");

    info!(
        types = rust_analyzer.type_count(),
        root = rust_analyzer.root_type.as_deref().unwrap_or("<none>"),
        "Starting RON LSP server"
    );

    roniker::serve(rust_analyzer, false).await;
}
