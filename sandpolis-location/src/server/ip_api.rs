use super::Locator;

/// Uses the ip-api.com web service to resolve IP locations.
pub struct IpApiLocator {
    key: Option<String>,
    client: reqwest::Client,
}

/// Query response from ip-api.com.
struct Response {
    status: String,
    continent: Option<String>,
    continentCode: Option<String>,
    country: Option<String>,
    countryCode: Option<String>,
    region: Option<String>,
    regionName: Option<String>,
    city: Option<String>,
    district: Option<String>,
    zip: Option<String>,
    lat: Option<f64>,
    lon: Option<f64>,
    timezone: Option<String>,
    offset: Option<i64>,
    currency: Option<String>,
    isp: Option<String>,
    org: Option<String>,
    r#as: Option<String>,
    asname: Option<String>,
    mobile: Option<bool>,
    proxy: Option<bool>,
    hosting: Option<bool>,
}

impl From<Response> for LocationData {
    fn from(value: Response) -> Self {
        todo!()
    }
}

impl Locator for IpApiLocator {
    async fn query(&self, ip: &str) -> Result<Option<LocationData>> {
        let response: Response = self
            .client
            .get(format!("ip-api.com/json/", ip))
            .send()
            .await?
            .json()?;

        Ok(Some(response.into()))
    }
}

#[cfg(test)]
mod test_ip_api {
    use super::*;

    #[test]
    fn test_query() {
        let locator = IpApiLocator::new();
    }
}
