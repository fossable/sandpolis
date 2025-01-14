pub trait Geolocator {
    async fn query(ip: &str) -> Result<LocationData>;
}
