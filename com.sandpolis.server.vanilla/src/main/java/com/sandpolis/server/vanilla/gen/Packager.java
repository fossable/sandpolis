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
package com.sandpolis.server.vanilla.gen;

import com.sandpolis.core.proto.util.Generator.GenConfig;

/**
 * A packager accepts a generator payload and produces the final output
 * according to the configuration.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class Packager {

	/**
	 * Convert the intermediate payload into a final output.
	 *
	 * @param config  The generator's configuration
	 * @param payload The intermediate payload
	 * @throws Exception
	 * @return The processed payload
	 */
	public abstract byte[] process(GenConfig config, Object payload) throws Exception;

	/**
	 * Get the {@link Packager}'s output file extension.
	 *
	 * @return A file extension
	 */
	public abstract String getFileExtension();

	/**
	 * This exception is thrown when a {@link Packager} receives the wrong payload.
	 */
	protected static class IncompatiblePayloadException extends RuntimeException {
		public IncompatiblePayloadException() {
		}
	}

}
