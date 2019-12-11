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
package com.sandpolis.core.stream.store;

import java.util.concurrent.SubmissionPublisher;

import com.google.protobuf.MessageOrBuilder;

/**
 * @author cilki
 * @since 5.0.2
 */
public abstract class StreamSource<E extends MessageOrBuilder> extends SubmissionPublisher<E>
		implements StreamEndpoint {

	private int id;

	@Override
	public int getStreamID() {
		return id;
	}

	/**
	 * Immediately halt the flow of events from the source.
	 */
	public abstract void stop();

	/**
	 * Begin the flow of events from the source.
	 */
	public abstract void start();

}
