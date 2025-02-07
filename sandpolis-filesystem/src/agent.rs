struct FilesystemSession {
    cwd: PathBuf,
    watcher: Box<dyn Watcher>,
}

#[axum_macros::debug_handler]
async fn filesystem_session(
    state: State<FilesystemLayer>,
    ws: WebSocketUpgrade,
    extract::Json(request): extract::Json<FilesystemSessionRequest>,
) -> Result<(), StatusCode> {
    let session = ShellSession::new(request).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    ws.on_upgrade(move |socket| session.run(socket));

    Ok(())
}

#[axum_macros::debug_handler]
async fn filesystem_delete(
    state: State<FilesystemLayer>,
    extract::Json(request): extract::Json<FilesystemDeleteRequest>,
) -> Result<Json<FilesystemDeleteResponse>, StatusCode> {
    todo!()
}
