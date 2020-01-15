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
package com.sandpolis.core.profile;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sandpolis.core.net.stream.StreamSource;
import com.sandpolis.core.profile.attribute.key.AttributeKey;
import com.sandpolis.core.proto.net.MsgAttribute.EV_AttributeStream;

public class AttributeStreamSource extends StreamSource<EV_AttributeStream> {

	private ScheduledFuture<?> task;

	private long period;

	private List<AttributeKey<?>> keys;

	public AttributeStreamSource(List<String> paths, long period) {
		// TODO
		this.period = period;
	}

	@Override
	public void stop() {
		if (task != null)
			task.cancel(true);
	}

	@Override
	public void start() {
		ScheduledExecutorService service = ThreadStore.get("attributes");
		task = service.scheduleAtFixedRate(() -> {
			submit(EV_AttributeStream.newBuilder().build());
		}, 0, period, TimeUnit.MILLISECONDS);

	}

}
