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
package com.sandpolis.server.vanilla.gen.packager;

import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.server.vanilla.gen.Packager;

/**
 * This {@link Packager} produces a runnable jar file.
 *
 * @author cilki
 * @since 5.0.0
 */
public class JarPackager extends Packager {
	private JarPackager() {
	}

	public static final JarPackager INSTANCE = new JarPackager();

	@Override
	public byte[] process(GenConfig config, Object payload) throws Exception {
		switch (config.getPayload()) {
		case OUTPUT_MEGA:
			return (byte[]) payload;
		default:
			throw new IncompatiblePayloadException();
		}
	}

	@Override
	public String getFileExtension() {
		return "jar";
	}

}
