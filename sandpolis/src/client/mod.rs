use anyhow::Result;

pub mod ui;

pub async fn main() -> Result<()> {
    crate::client::ui::run();
    Ok(())
}
