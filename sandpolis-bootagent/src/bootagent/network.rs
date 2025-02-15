use smoltcp::phy::{Device, DeviceCapabilities, ChecksumCapabilities};
use smoltcp::phy;
use uefi::proto::network::snp::Snp;
use uefi::proto::network::snp::MacAddress;
use uefi::Result;
use core::ptr;
use smoltcp::time::Instant;
use alloc::vec::Vec;

const IPV4_PROTOCOL : u16 = 2048;

pub struct SnpDevice {
    snp: Snp,
}

impl SnpDevice {
    pub fn new(snp: Snp) -> SnpDevice {
        SnpDevice {
            snp: snp
        }
    }
}

impl<'a> Device<'a> for SnpDevice {
    type RxToken = RxToken;
    type TxToken = TxToken;

    fn capabilities(&self) -> DeviceCapabilities {
        DeviceCapabilities {
            max_transmission_unit: self.snp.mode().max_packet_size() as usize,
            max_burst_size: None,
            checksum: ChecksumCapabilities::ignored(),
        }
    }

    fn receive(&'a mut self) -> Option<(Self::RxToken, Self::TxToken)> {

        let mut header_size = 14usize;
        let mut buffer_size = self.snp.mode().max_packet_size() as usize;
        let mut buffer = vec![0; buffer_size];

        match self.snp.receive(
            &mut header_size as *mut usize,
            &mut buffer_size,
            &mut buffer[..],
            ptr::null_mut(),
            ptr::null_mut(),
            ptr::null_mut(),
        ) {
            Ok(size) => {
                buffer.resize(buffer_size, 0);
                let rx = RxToken { buffer };
                let tx = TxToken {
                    snp: &self.snp,
                };
                Some((rx, tx))
            }
            Err(err) => None,
        }

    }

    fn transmit(&'a mut self) -> Option<Self::TxToken> {
        Some(TxToken {
            snp: &self.snp,
        })
    }
}

#[doc(hidden)]
pub struct RxToken {
    buffer: Vec<u8>,
}

impl phy::RxToken for RxToken {
    fn consume<R, F>(mut self, _timestamp: Instant, f: F) -> Result<R>
    where
        F: FnOnce(&mut [u8]) -> Result<R>,
    {
        f(&mut self.buffer[..])
    }
}

#[doc(hidden)]
pub struct TxToken<'a> {
    snp: &'a Snp,
}

impl phy::TxToken for TxToken<'_> {
    fn consume<R, F>(self, _timestamp: Instant, len: usize, f: F) -> Result<R>
    where
        F: FnOnce(&mut [u8]) -> Result<R>,
    {
        let header_size = 14usize;
        let mut buffer = vec![0; len];
        let mut src_addr = self.snp.mode().current_address();
        let result = f(&mut buffer);
        self.snp.transmit(
            header_size,
            len,
            &buffer[..],
            &src_addr as *const MacAddress,
            ptr::null_mut(),
            &IPV4_PROTOCOL as *const u16,
        );
        result
    }
}
