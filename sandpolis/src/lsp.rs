use roniker::{serve, RustAnalyzer};

pub async fn run() {

    let rust_analyzer: RustAnalyzer = serde_json::from_str(include_str!(concat!(
        env!("OUT_DIR"),
        "/rust_analyzer.json"
    )))
    .expect("Failed to deserialize RustAnalyzer");

    roniker::serve(rust_analyzer).await;
}
