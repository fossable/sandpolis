use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use tracing::info;

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    let network = state.network.clone();
    // TODO connection attempts

    Ok(())
}
