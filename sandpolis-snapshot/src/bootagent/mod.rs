use crate::core::net::message::MSG;
use crate::lib::connection::Connection;
use crate::plugin::snapshot::msg_snapshot::*;
use anyhow::Error;
use fasthash::murmur3;
use log::{debug, error};

pub struct BlockSnapshotMetadata {}

pub struct BlockSnapshotter<'a> {
    pub block_size: usize,
    pub connection: &'a mut Connection,
}

impl BlockSnapshotter<'_> {
    pub fn read(&mut self, device_path: String) -> Result<(), Error> {
        let device = File::open(device_path)?;
        let data = unsafe { MmapOptions::new().map(&device)? };

        for i in (0_usize..device.metadata()?.len() as usize).step_by(self.block_size) {
            let block = &data[i..(i + self.block_size)];
            let hash = murmur3::hash128(&block);

            // TODO check with metadata before sending

            // Send update
            let mut ev_snapshot = EV_SnapshotDataBlock::new();
            ev_snapshot.data = block.to_vec();
            let ev = MSG::new();
            self.connection.send(&ev);
        }
        return Ok(());
    }
}
