#[derive(Parser, Debug, Clone)]
pub struct ServerCommandLine {
    /// Server listen address:port
    #[clap(long, default_value_t = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(0, 0, 0, 0)), ServerAddress::default_port()))]
    pub listen: SocketAddr,

    /// Run as a local stratum (LS) server instead of in the global stratum (GS).
    #[clap(long)]
    pub local: bool,
}
