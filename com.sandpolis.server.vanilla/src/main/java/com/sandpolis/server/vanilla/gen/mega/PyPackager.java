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
package com.sandpolis.server.vanilla.gen.mega;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.server.vanilla.gen.MegaGen;

/**
 * This generator produces a Python script.
 *
 * @author cilki
 * @since 5.0.0
 */
public class PyPackager extends MegaGen {
	public PyPackager(GenConfig config) {
		super(config);
	}

	@Override
	protected byte[] generate() throws Exception {
		Map<String, String> cfg = new HashMap<>();

		String stub = CharStreams
				.toString(new InputStreamReader(PyPackager.class.getResourceAsStream("stub.py"), Charsets.UTF_8));
		stub.replaceFirst("# PLACEHOLDER",
				cfg.entrySet().stream()
						.map(entry -> String.format("config['%s'] = '%s'%n", entry.getKey(), entry.getValue()))
						.collect(Collectors.joining()));

		return stub.getBytes();
	}

}
