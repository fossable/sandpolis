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
package com.sandpolis.viewer.cmd;

import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCGenerator.RQ_Generate;
import com.sandpolis.core.proto.net.MCGenerator.RS_Generate;
import com.sandpolis.core.proto.util.Generator.GenConfig;

/**
 * Contains generator commands.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class GenCmd extends Cmdlet<GenCmd> {

	/**
	 * Generate a payload from the given configuration.
	 * 
	 * @param config The generation configuration
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<RS_Generate> generate(GenConfig config) {
		return rq(RQ_Generate.newBuilder().setConfig(config));
	}

	/**
	 * Prepare for an asynchronous command.
	 * 
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link GenCmd} can be invoked
	 */
	public static GenCmd async() {
		return new GenCmd();
	}

	private GenCmd() {
	}
}
