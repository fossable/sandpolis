//! # Boot Agent
//!
//! The boot agent is a special type of agent instance that runs as a custom UEFI
//! application rather than on a host's OS.
//!
//! ## Instance Configuration
//!
//! ```py
//! # com.sandpolis.agent.boot
//! {
//!   "graphics": {
//!     "enabled" : Boolean(), # Whether graphics are enabled
//!     "width"   : Number(),
//!     "height"  : Number(),
//!   },
//!   "network": {
//!     "mac_address" : String(), # The MAC address of the target NIC
//!     "ip_address"  : String(), # The IP address of the target NIC
//!   }
//! }
//! ```
//!
//! ## Snapshot Plugin
//!
//! The boot agent is able to create and apply _consistent_ filesystem snapshots to
//! any partition. This is because no partitions are in use while the boot agent is
//! running.
//!
//! ## Installation
//!
//! | Architecture | ESP Path               |
//! | ------------ | ---------------------- |
//! | X86_64       | `/EFI/Boot/S7Sx64.efi` |
//! | AArch64      |                        |
//!
//! ### Boot Wait
//!
//! ```
//!  ┌────────────────────────────────────────────────┐
//!  │                                                │
//!  │              ┌──────────────────┐              │
//!  │              │      Image       │              │
//!  │              │                  │              │
//!  │              └──────────────────┘              │
//!  │                                                │
//!  │                Current Status                  │
//!  │                                                │
//!  │                                                │
//!  │                                                │
//!  └────────────────────────────────────────────────┘
//! ```
//!
//! ### Snapshot Operation
//!
//! ```
//!  ┌────────────────────────────────────────────────┐
//!  │:Block:Visualizer:::::::::::::::::::::::::::::::│
//!  │::::::::::::::::::::::::::::::::::::::::::::::::│
//!  │::::::::::::::┌──────────────────┐::::::::::::::│
//!  │::::::::::::::│      Image       │::::::::::::::│
//!  │::::::::::::::├──────────────────┤::::::::::::::│
//!  │::::::::::::::│ Transfer Stats   │::::::::::::::│
//!  │::::::::::::::│                  │::::::::::::::│
//!  │::::::::::::::└──────────────────┘::::::::::::::│
//!  │::::::::::::::::::::::::::::::::::::::::::::::::│
//!  │::::::::::::::::::::::::::::::::::::::::::::::::│
//!  └────────────────────────────────────────────────┘
//! ```

fn main() {
    println!("Hello, world!");
}
