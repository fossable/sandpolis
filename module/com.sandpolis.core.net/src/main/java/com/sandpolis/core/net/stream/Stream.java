//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.stream;

import com.sandpolis.core.foundation.util.IDUtil;

/**
 * A {@link Stream} is an ephemeral flow of events between two endpoints in the
 * network.
 *
 * @author cilki
 * @since 2.0.0
 */
public class Stream {

	private int streamID;

	public Stream() {
		streamID = IDUtil.stream();
	}

	public int getStreamID() {
		return streamID;
	}
}
