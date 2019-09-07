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
import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.command.CommandFuture;
import com.sandpolis.core.proto.net.MCPlugin.RQ_ArtifactDownload;
import com.sandpolis.core.proto.net.MCPlugin.RQ_PluginList;
import com.sandpolis.core.proto.net.MCPlugin.RS_ArtifactDownload;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginList;
import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.core.util.NetUtil;

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
	public CommandFuture sync() {
		var session = begin();

		session.request(RQ_PluginList.newBuilder(), (RS_PluginList rs) -> {
			rs.getPluginList().stream().filter(descriptor -> {
				Optional<Plugin> plugin = PluginStore.getPlugin(descriptor.getId());
				if (plugin.isEmpty())
					return true;

				// Check versions
				return !plugin.get().getVersion().equals(descriptor.getVersion());
			}).forEach(descriptor -> {
				session.sub(install(descriptor.getCoordinate() + ":" + descriptor.getVersion()));
			});
		});

		return session;
	}

	/**
	 * Download a plugin to the plugin directory.
	 * 
	 * @param gav The plugin coordinate
	 * @return The command future
	 */
	public CommandFuture install(String gav) {
		checkNotNull(gav);
		var session = begin();

		session.sub(installDependency(gav), outcome -> {
			PluginStore.installPlugin(Environment.get(LIB).resolve(fromCoordinate(gav).filename));
		});

		return session;
	}

	/**
	 * Download a dependency to the library directory.
	 * 
	 * @return The command future
	 */
	public CommandFuture installDependency(String gav) {
		checkNotNull(gav);
		var session = begin();

		Path destination = Environment.get(LIB).resolve(fromCoordinate(gav).filename);
		if (Files.exists(destination))
			// Nothing to do
			return session.success();

		var rq = RQ_ArtifactDownload.newBuilder().setCoordinates(gav).setLocation(false);

		session.request(rq, (RS_ArtifactDownload rs) -> {

			switch (rs.getSourceCase()) {
			case BINARY:
				Files.write(destination, rs.getBinary().toByteArray());
				break;
			case COORDINATES:
				ArtifactUtil.download(destination.getParent(), rs.getCoordinates());
				break;
			case URL:
				NetUtil.download(rs.getUrl(), destination.toFile());
				break;
			default:
				session.abort("Unknown source case: " + rs.getSourceCase());
				return;
			}

			try {
				// Get any missing dependencies recursively
				SoiUtil.getMatrix(destination).getAllDependencies()
						.forEach(dep -> session.sub(installDependency(dep.getCoordinates())));
			} catch (NoSuchFileException e) {
				// This dependency does not have a soi/matrix.bin (skip it)
			}
		});

		return session;
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
