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
package com.sandpolis.core.net.state;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

public abstract class EntangledObject<T extends Message> extends AbstractSTObject {

	protected StreamSink<T> sink;
	protected StreamSource<T> source;

	public StreamSource<T> getSource() {
		return source;
	}

	public StreamSink<T> getSink() {
		return sink;
	}
}
