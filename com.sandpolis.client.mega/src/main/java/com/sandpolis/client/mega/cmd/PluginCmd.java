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

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Environment.EnvPath;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.future.CommandFuture;
import com.sandpolis.core.proto.net.MCPlugin.RQ_PluginDownload;
import com.sandpolis.core.proto.net.MCPlugin.RQ_PluginList;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginDownload;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginList;

/**
 * Contains plugin commands.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class PluginCmd extends Cmdlet<PluginCmd> {

	/**
	 * Initiate plugin synchronization. Any plugins that are missing from the
	 * {@link PluginStore} will be downloaded and installed (but not loaded).
	 * 
	 * @return The command future
	 */
	public CommandFuture beginSync() {

		return rq(RQ_PluginList.newBuilder(), (RS_PluginList rs) -> {
			rs.getPluginList().stream()
					// Skip up-to-date plugins
					.filter(descriptor -> {
						Optional<Plugin> plugin = PluginStore.getPlugin(descriptor.getId());
						if (plugin.isEmpty())
							return true;

						// Check versions
						return !plugin.get().getVersion().equals(descriptor.getVersion());
					}).forEach(descriptor -> {
						try {
							downloadPlugin(descriptor.getId()).sync();
						} catch (InterruptedException e) {
							commandFuture.setFailure(e);
							// TODO stop executing
						}
					});
		});
	}

	/**
	 * Download a plugin to the plugin directory.
	 * 
	 * @return The command future
	 */
	public CommandFuture downloadPlugin(String id) {
		checkNotNull(id);

		return rq(RQ_PluginDownload.newBuilder().setId(id).setLocation(false), (RS_PluginDownload rs) -> {

			Path destination = Environment.get(EnvPath.JLIB).resolve(id + ".jar");

			switch (rs.getSourceCase()) {
			case PLUGIN_BINARY:
				Files.write(destination, rs.getPluginBinary().toByteArray());
				break;
			case PLUGIN_COORDINATES:
				throw new RuntimeException("Unimplemented");
			case PLUGIN_URL:
				throw new RuntimeException("Unimplemented");
			default:
				throw new RuntimeException();
			}

			// TODO download missing dependencies

			PluginStore.installPlugin(destination);
		});
	}

	/**
	 * Download a dependency to the library directory.
	 * 
	 * @return The command future
	 */
	public CommandFuture downloadDependency(String coordinate) {
		checkNotNull(coordinate);

		// TODO
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
