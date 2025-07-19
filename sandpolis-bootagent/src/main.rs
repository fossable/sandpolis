#![no_std]
#![no_main]
#![feature(asm)]
#![feature(abi_efiapi)]

use serde::Deserialize;
use alloc::string::String;
use core::mem;
use uefi::prelude::*;
use uefi::proto::console::serial::Serial;
use uefi::proto::pi::mp::MpServices;
use uefi::table::boot::MemoryDescriptor;
use uefi::proto::network::snp::Snp;
use uefi::table::runtime::ResetType;
use crate::network::SnpDevice;


#[derive(Deserialize)]
struct BuildJson {

}

const BUILD_DATA: &str = "b215dfd2febd88cee51ef64e864827e959ed2e8e44aaff31dd3bc8153380ab9a894f24511aad3099b31aa5c3ed1137e61361e0f5405a486acc88f97c27ce71476e4afb7aee44456a714e0b5de443ffc1d509680a2b464b30c0ab90509e2aaafe0498201d5792a8757a21d738820c38a6ddd45c50b6afacfc7e3ad5eeed1df6df7591917f68803337a0e57dff30ed9901e4e8a890e4f8c2fcbfcc00d38f23678deb28ca7d342e22570c2e62641b712f77388bbb36dc585756906cc4d4010b6ad0f72a67708891a0f20b9419853eaafcf26f0e914cefbc008342a7d675ebb275a26d0cdcfb2968695b0ac2b7ee40bcaa1c494e96ec53cecadb524c";

#[derive(Deserialize)]
struct GraphicsConfig {
    enabled: bool,
    width: usize,
    height: usize,
}

// The 500-byte config placeholder
const AGENT_DATA: &str = "577ed79e3db940b5576cbba18ee5cfe73397c4c2fcabd451185260ecf135716b139456ce6d8c353537af0b93085da8496dcdeeea2fbbfd7e10dfbbfaeb35bd77f145988cbe4063245213bf2205b28b5d156258f18796f90f0f8c50bdd7451bcb01842da992004e79914394c96ed522a436ede7d681dfb33eaa4bc25f070a063fcb3e1a2bff7a35663c89ae2a1af7d29ae659c558fdf7c2f37d178113478a3113a9fa277597114ed9bbc3689a2332ec2e18742b34514a45146ccd04d5d675ec385334a018cbcad51b2d145f9abdc9153368f41823b263bfa1eae4b6281f0fc5fe6d52763f312a15ec48e926176c3074f5d127883bf7c73fa45755";

fn parse_mac(mac_str: &str) -> [u8; 6] {
    let mut mac = [0u8; 6];
    let mut i = 0;

    for byte_str in mac_str.split(":") {
        if let Ok(byte) = byte_str.parse::<u8>() {
            mac[i] = byte;
            i++;
        }
    }

    return mac;
}

#[entry]
fn efi_main(image: Handle, mut st: SystemTable<Boot>) -> Status {

    // Initialize UEFI services
    uefi_services::init(&mut st).expect_success("Failed to initialize utilities");

    // Clear the screen
    st.stdout()
        .reset(false)
        .expect_success("Failed to reset stdout");

    // Read build config
    let build: BuildJson = serde_json::from_str(&BUILD_DATA).expect("Invalid build config");

    // Read instance config
    let agent_config: AgentConfig = serde_json::from_str(&AGENT_DATA).expect("Invalid agent config");

    // Retrieve firmware vendor
    let mut vendor = String::new();
    st.firmware_vendor().as_str_in_buf(&mut vendor).unwrap();

    let bt = st.boot_services();
    let rt = unsafe { st.runtime_services() };

    // Check minimum number of processors
    if let Ok(mp) = bt.locate_protocol::<MpServices>() {
        let mp = mp.expect("Failed to load MpServices protocol");
        let mp = unsafe { &mut *mp.get() };

        if let Ok(processor_count) = mp.get_number_of_processors() {
            if processor_count.result.total < 4 {
                panic!("At least 4 processor cores are required");
            }
        }
    } else {
        panic!("MpServices protocol is not available");
    }

    // Find the configured network interface
    let nic_handles = bt
        .find_handles::<Snp>()
        .expect_success("Failed to get handles for `Snp` protocol");

    let mut device: SnpDevice;

    for nic_handle in nic_handles {
        let nic = bt.handle_protocol::<Snp>(nic_handle).expect_success("Unknown");
        let nic = unsafe { &*nic.get() };

        if nic.mode().current_address().addr[0 .. 6] == parse_mac(agent_config.mac) {
            debug!("Found NIC");
            device = SnpDevice::new(nic);
        }
    }

    // Initialize UI if enabled
    if agent_config.graphics.enabled {
        MainUI::new(agent_config.graphics.width, agent_config.graphics.height);
    }

    loop {
        // Connection attempt
        if let Ok(connection) = Connection::new(&device) {

            loop {
                // Message dispatch
                if let Ok(message) = connection.read() {

                    match message.payload_type() {
                        payload_type!("RQ_CreateSnapshot"): => {

                        },
                        payload_type!("RQ_ApplySnapshot"): => {
                            
                        },
                    }
                } else {
                    break;
                }
            }
        }

        // Wait before trying again
        bt.stall(100_000);
    }

    rt.reset(ResetType::Shutdown, Status::SUCCESS, None);
}

