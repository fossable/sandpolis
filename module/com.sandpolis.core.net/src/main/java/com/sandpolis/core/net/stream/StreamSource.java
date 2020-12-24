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

import java.util.concurrent.SubmissionPublisher;

import com.google.protobuf.MessageLiteOrBuilder;

/**
 * @author cilki
 * @since 5.0.2
 */
public abstract class StreamSource<E extends MessageLiteOrBuilder> extends SubmissionPublisher<E>
		implements StreamEndpoint {

	private int id;

	@Override
	public int getStreamID() {
		return id;
	}

	/**
	 * Begin the flow of events from the source.
	 */
	public abstract void start();

	/**
	 * Immediately halt the flow of events from the source.
	 */
	public abstract void stop();

}
