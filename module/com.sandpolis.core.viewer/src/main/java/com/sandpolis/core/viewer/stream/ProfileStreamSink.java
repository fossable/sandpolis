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
package com.sandpolis.core.viewer.stream;

import com.sandpolis.core.proto.net.MsgStream.EV_ProfileStream;
import com.sandpolis.core.stream.store.StreamSink;

public class ProfileStreamSink extends StreamSink<EV_ProfileStream> {

	@Override
	public void onNext(EV_ProfileStream item) {
		super.onNext(item);

		// TODO update profile store
	}
}
