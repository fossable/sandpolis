use core_protocol::core::protocol::GetBannerResponse;

pub struct Banner {
    pub maintenance: bool,

    pub version: String,

    pub message: String,

    pub image: Vec<u8>,
}

impl Into<GetBannerResponse> for Banner {
    fn into(self) -> GetBannerResponse {
        GetBannerResponse {
            maintenance: false,
            version: self.version,
            message: self.message,
            image: self.image,
        }
    }
}
