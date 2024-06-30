//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

use crate::core::net::message::MSG;
use protobuf::Message;

pub fn rq(payload: &Message) -> MSG {
	let mut rq = MSG::new();
	// TODO
	return rq;
}
