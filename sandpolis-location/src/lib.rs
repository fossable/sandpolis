#[cfg(feature = "server")]
pub mod server;

pub struct LocationData {
    /// The AS name
    pub as_name: String,
    /// The numerical AS code
    pub as_code: u32,
    /// The city name
    pub city: String,
    /// The continent name
    pub continent: String,
    /// The ISO continent code
    pub continent_code: String,
    /// The country name
    pub country: String,
    /// Two-letter country code (ISO 3166-1 alpha-2)
    pub country_code: String,
    /// The currency name
    pub currency: String,
    /// The city district name
    pub district: String,
    /// The Internet Service Provider name
    pub isp: String,
    /// The approximate latitude in degrees
    pub latitude: f32,
    /// The approximate longitude in degrees
    pub longitude: f32,
    /// The metro code
    pub metro_code: u32,
    /// The organization name
    pub organization: String,
    /// The zip code
    pub postal_code: String,
    /// The region name
    pub region: String,
    /// The region short code (FIPS or ISO)
    pub region_code: String,
    /// The timezone name
    pub timezone: String,
}
