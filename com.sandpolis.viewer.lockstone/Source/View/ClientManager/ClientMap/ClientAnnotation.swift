//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
import MapKit

class ClientAnnotation: NSObject, MKAnnotation {

	var coordinate: CLLocationCoordinate2D
	var title: String?

	let profile: SandpolisProfile

	init?(_ profile: SandpolisProfile) {

		self.profile = profile
		self.title = profile.hostname
		if let latitude = profile.location?.latitude, let longitude = profile.location?.longitude {
			self.coordinate = CLLocationCoordinate2DMake(CLLocationDegrees(latitude), CLLocationDegrees(longitude))
		} else {
			return nil
		}
	}
}
