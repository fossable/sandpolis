//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.stream;

import java.util.concurrent.SubmissionPublisher;
import java.util.random.RandomGenerator;

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.foundation.S7SRandom;
import org.s7s.core.instance.stream.StreamEndpoint.StreamPublisher;

/**
 * @author cilki
 * @since 5.0.2
 */
public abstract class StreamSource<E extends MessageLiteOrBuilder> extends SubmissionPublisher<E>
		implements StreamPublisher<E> {

	private int id;

	public StreamSource() {
		id = S7SRandom.insecure.nextInt();
		id = RandomGenerator.getDefault().nextInt();
	}

	@Override
	public int getStreamID() {
		return id;
	}

	/**
	 * Begin the flow of events from the source.
	 */
	public abstract void start();

}
