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
package com.sandpolis.plugin.desktop.cmd;

import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.plugin.desktop.net.MCDesktop.RQ_Screenshot;
import com.sandpolis.plugin.desktop.net.MCDesktop.RS_Screenshot;

/**
 * Contains desktop commands.
 *
 * @author cilki
 * @since 5.0.2
 */
public final class DesktopCmd extends Cmdlet<DesktopCmd> {

	/**
	 * Take a desktop screenshot.
	 *
	 * @return A response future
	 */
	public ResponseFuture<RS_Screenshot> screenshot() {
		return request(RQ_Screenshot.newBuilder());
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link DesktopCmd} can be invoked
	 */
	public static DesktopCmd async() {
		return new DesktopCmd();
	}

	private DesktopCmd() {
	}
}
