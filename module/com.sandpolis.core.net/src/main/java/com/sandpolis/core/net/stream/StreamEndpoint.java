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

public interface StreamEndpoint {

	/**
	 * Get the StreamID of the stream that this endpoint is a member of.
	 *
	 * @return The endpoint's stream ID
	 */
	public int getStreamID();

}
