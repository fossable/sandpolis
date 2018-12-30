/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.server.gen.packager;

import static com.sandpolis.core.instance.Environment.EnvPath.GEN;

import java.nio.file.Files;

import com.google.common.io.BaseEncoding;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Generator.MicroConfig;
import com.sandpolis.server.gen.Packager;

/**
 * This {@link Packager} produces a URL with an embedded configuration.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class UrlPackager extends Packager {
	private UrlPackager() {
	}

	public static final UrlPackager INSTANCE = new UrlPackager();

	@Override
	public void process(GenConfig config, Object payload) throws Exception {
		String url = "https://sandpolis.com/config?c=";

		switch (config.getPayload()) {
		case OUTPUT_CONFIG:
			switch (config.getPayloadConfigCase()) {
			case MEGA:
				url += BaseEncoding.base64Url().encode(((MegaConfig) payload).toByteArray());
				break;
			case MICRO:
				url += BaseEncoding.base64Url().encode(((MicroConfig) payload).toByteArray());
				break;
			default:
				throw new RuntimeException();
			}
			break;
		default:
			throw new IncompatiblePayloadException();
		}

		// Write to generator output directory
		Files.write(Environment.get(GEN).resolve(config.getId() + ".url"), url.getBytes());
	}
}
