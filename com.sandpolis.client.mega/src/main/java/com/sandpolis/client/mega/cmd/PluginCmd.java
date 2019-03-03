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
package com.sandpolis.client.mega.cmd;

import java.util.Optional;

import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCPlugin.RQ_PluginDownload;
import com.sandpolis.core.proto.net.MCPlugin.RQ_PluginList;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginList;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * Contains plugin commands.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class PluginCmd extends Cmdlet<PluginCmd> {

	/**
	 * Initiate a plugin synchronization.
	 * 
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> beginSync() {

		rq(RQ_PluginList.newBuilder()).addListener(rs -> {

			((RS_PluginList) rs.get()).getPluginList().stream()
					// Skip up-to-date plugins
					.filter(descriptor -> {
						Optional<Plugin> plugin = PluginStore.getPlugin(descriptor.getId());
						if (plugin.isEmpty())
							return true;

						// TODO compare semantic versions correctly
						return (plugin.get().getVersion().compareTo(descriptor.getVersion()) < 0);
					}).forEach(descriptor -> {
						rq(RQ_PluginDownload.newBuilder().setId(descriptor.getId())).addListener(rs_download -> {
							// TODO
						});
					});
		});

		return null;
	}

	/**
	 * Prepare for an asynchronous command.
	 * 
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link PluginCmd} can be invoked
	 */
	public static PluginCmd async() {
		return new PluginCmd();
	}

	private PluginCmd() {
	}
}
