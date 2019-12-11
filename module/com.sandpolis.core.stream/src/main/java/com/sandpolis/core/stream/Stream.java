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
package com.sandpolis.core.stream;

import com.sandpolis.core.util.IDUtil;

/**
 * @author cilki
 * @since 5.0.2
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
