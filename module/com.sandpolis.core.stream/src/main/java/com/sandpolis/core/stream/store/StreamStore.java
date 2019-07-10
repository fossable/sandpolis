/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.stream.store;

import java.util.LinkedList;
import java.util.List;

import com.sandpolis.core.instance.Store.AutoInitializer;
import com.sandpolis.core.proto.net.MCStream.EV_StreamData;

/**
 * There are four "banks" that each serve a specific purpose.
 * 
 * <pre>
 * IN     OUT    SRC    SINK
 * [A]    [B]    [B]    [A]
 * [ ]    [A]    [ ]    [ ]
 * [ ]    [A]    [ ]    [ ]
 * [ ]    [ ]    [ ]    [ ]
 * [ ]    [ ]    [ ]    [ ]
 * </pre>
 * 
 * @author cilki
 * @since 5.0.2
 */
@AutoInitializer
public final class StreamStore {

	/**
	 * The SOURCE bank.
	 */
	static List<StreamSource<?>> source = new LinkedList<>();

	/**
	 * The SINK bank.
	 */
	static List<StreamSink<?>> sink = new LinkedList<>();

	/**
	 * The INBOUND bank.
	 */
	static List<InboundStreamAdapter<?>> inbound = new LinkedList<>();

	/**
	 * The OUTBOUND bank.
	 */
	static List<OutboundStreamAdapter<?>> outbound = new LinkedList<>();

	public static void streamData(EV_StreamData data) {
		for (var adapter : inbound) {
			if (adapter.getStreamID() == data.getId()) {
				adapter.submit(data);
				break;
			}
		}
	}

	public static void stop(int streamID) {
//		streams.stream().filter(s -> s.getId() == streamID).findAny().ifPresent(s -> {
//			s.getDownstream().forEach(DownstreamEndpoint::close);
//		});
	}

}
