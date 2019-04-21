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
package com.sandpolis.server.exe;

import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCGenerator.RS_Generate;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.server.PoolConstant.server;
import com.sandpolis.server.gen.FileGenerator;
import com.sandpolis.server.gen.generator.MegaGen;

/**
 * Generator message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class GenExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(GenExe.class);

	public GenExe(Sock connector) {
		super(connector);
	}

	@Auth
	public void rq_generate(Message m) throws Exception {
		var config = m.getRqGenerate().getConfig();

		ExecutorService pool = ThreadStore.get(server.generator);
		FileGenerator generator = pool.submit(() -> {
			FileGenerator gen;
			switch (config.getPayload()) {
			case OUTPUT_MEGA:
				gen = new MegaGen(config);
				break;
			case OUTPUT_MICRO:
			default:
				log.warn("Failed to execute generator of type: {}", config.getPayload());
				return null;
			}

			try {
				gen.generate();
			} catch (Exception e) {
				log.error("Failed to generate", e);
			}

			return gen;
		}).get();

		if (generator == null)
			reply(m, Outcome.newBuilder().setResult(false));
		else if (generator.getResult() != null)
			try (var in = new FileInputStream(generator.getResult())) {
				reply(m, RS_Generate.newBuilder().setReport(generator.getReport()).setOutput(ByteString.readFrom(in)));
			}
		else
			reply(m, RS_Generate.newBuilder().setReport(generator.getReport()));

	}

}
