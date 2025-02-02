#[debug_handler]
async fn power(
    state: State<AgentState>,
    extract::Json(request): extract::Json<PowerRequest>,
) -> Result<Json<PowerResponse>, Json<PowerResponse>> {
    // libc::reboot
    todo!()
}

pub fn router() -> Router<AgentState> {
    Router::new().route("/power", post(power))
}
