use tokio::process::Child;

pub struct ShellSession {
    pub data: ShellSessionData,
    pub process: Child,
}

impl StreamSource<ShellSessionOutputEvent> for ShellSession {
    async fn emit(&self) -> ShellSessionOutputEvent {}
}

impl StreamSink<ShellSessionInputEvent> for ShellSession {
    async fn accept(&self, event: ShellSessionInputEvent) -> Result<ShellSessionOutputEvent> {}
}

impl Drop for ShellSession {
    fn drop(&mut self) {
        debug!("Killing child process");
        self.process.kill()
    }
}
