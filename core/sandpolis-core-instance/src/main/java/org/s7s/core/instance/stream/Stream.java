//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.stream;

import org.s7s.core.foundation.S7SRandom;

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
		streamID = S7SRandom.nextNonzeroInt();
	}

	public int getStreamID() {
		return streamID;
	}
}
