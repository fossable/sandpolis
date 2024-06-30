
pub struct GeolocationFuture {

}

impl Future for GeolocationFuture {
	
}

pub trait Geolocator {
	fn query(ip: String) -> GeolocationFuture;
}