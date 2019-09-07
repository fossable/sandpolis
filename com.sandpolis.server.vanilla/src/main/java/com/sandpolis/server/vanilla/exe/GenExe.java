/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCGenerator.RQ_Generate;
import com.sandpolis.core.proto.net.MCGenerator.RS_Generate;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.server.vanilla.PoolConstant.server;
import com.sandpolis.server.vanilla.gen.FileGenerator;
import com.sandpolis.server.vanilla.gen.generator.MegaGen;

/**
 * Generator message handlers.
 *
 * @author cilki
 * @since 5.0.0
 */
public class GenExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(GenExe.class);

	@Auth
	@Handler(tag = MSG.Message.RQ_GENERATE_FIELD_NUMBER)
	public Message.Builder rq_generate(RQ_Generate rq) throws Exception {
		ExecutorService pool = ThreadStore.get(server.generator);

		Future<Message.Builder> future = pool.submit(() -> {
			var outcome = begin();

			FileGenerator generator;
			switch (rq.getConfig().getPayload()) {
			case OUTPUT_MEGA:
				generator = new MegaGen(rq.getConfig());
				break;
			case OUTPUT_MICRO:
			default:
				log.warn("No generator found for type: {}", rq.getConfig().getPayload());
				return failure(outcome);
			}

			try {
				generator.generate();
			} catch (Exception e) {
				log.error("Generation failed", e);
				return failure(outcome);
			}

			var rs = RS_Generate.newBuilder().setReport(generator.getReport());
			if (generator.getResult() != null)
				rs.setOutput(ByteString.copyFrom(generator.getResult()));

			return rs;
		});

		return future.get();
	}

}
