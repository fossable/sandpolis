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
package com.sandpolis.core.server.generator;

import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.Metatypes.InstanceType.VIEWER;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.sv.msg.MsgGenerator.RQ_Generate;
import com.sandpolis.core.sv.msg.MsgGenerator.RS_Generate;

/**
 * {@link GeneratorExe} contains message handlers related to generators.
 *
 * @since 5.0.0
 */
public final class GeneratorExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(GeneratorExe.class);

	@Handler(auth = true, instances = VIEWER)
	public static MessageOrBuilder rq_generate(RQ_Generate rq) throws Exception {
		ExecutorService pool = ThreadStore.get("server.generator");

		Future<MessageOrBuilder> future = pool.submit(() -> {
			var outcome = begin();

			Generator generator;
			switch (rq.getConfig().getPayload()) {
			case OUTPUT_MEGA:
				generator = MegaGen.build(rq.getConfig());
				break;
			case OUTPUT_MICRO:
			default:
				log.warn("No generator found for type: {}", rq.getConfig().getPayload());
				return failure(outcome);
			}

			generator.run();

			var rs = RS_Generate.newBuilder().setReport(generator.getReport());
			if (generator.getResult() != null)
				rs.setOutput(ByteString.copyFrom(generator.getResult()));

			return rs;
		});

		return future.get();
	}

	private GeneratorExe() {
	}
}
