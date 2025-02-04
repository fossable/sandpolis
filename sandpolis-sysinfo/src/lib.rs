#[cfg(feature = "agent")]
pub mod agent;

pub mod hardware;
pub mod os;

#[derive(Serialize, Deserialize, Default)]
pub struct SysinfoData {}

pub struct SysinfoLayer {
    pub data: Document<SysinfoData>,
    #[cfg(feature = "agent")]
    pub memory: MemoryMonitor,
}

impl SysinfoLayer {
    pub fn new(data: Document<SysinfoData>) -> Result<Self> {
        Ok(Self {
            memory: MemoryMonitor::new(data.document("/memory")?),
            data,
        })
    }
}
