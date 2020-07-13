//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.cv.cmd;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.foundation.soi.SoiUtil;
import com.sandpolis.core.foundation.util.ArtifactUtil;
import com.sandpolis.core.foundation.util.NetUtil;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.msg.MsgPlugin.RQ_ArtifactDownload;
import com.sandpolis.core.instance.msg.MsgPlugin.RQ_PluginOperation;
import com.sandpolis.core.instance.msg.MsgPlugin.RQ_PluginOperation.PluginOperation;
import com.sandpolis.core.instance.msg.MsgPlugin.RS_ArtifactDownload;
import com.sandpolis.core.instance.msg.MsgPlugin.RS_PluginSync;
import com.sandpolis.core.instance.plugin.PluginStore;
import com.sandpolis.core.instance.plugin.Plugin;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.command.CommandFuture;

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
	 * @return A new {@link CommandFuture}
	 */
	public CommandFuture<Outcome> synchronize() {
		var session = begin(Outcome.class);

		var rq = RQ_PluginOperation.newBuilder().setOperation(PluginOperation.PLUGIN_SYNC);

		session.request(rq).handle(RS_PluginSync.class, rs -> {
			rs.getPluginConfigList().stream().filter(config -> {
				Optional<Plugin> plugin = PluginStore.getByPackageId(config.getPackageId());
				if (plugin.isEmpty())
					return true;

				// Check versions
				return !plugin.get().getVersion().equals(config.getVersion());
			}).forEach(config -> {
				if (PluginStore.getByPackageId(config.getPackageId()).isEmpty()) {
					switch (config.getSourceCase()) {
					case PLUGIN_BINARY:
						// TODO
						break;
					case PLUGIN_COORDINATES:
						session.sub(install(config.getPluginCoordinates()));
						break;
					case PLUGIN_URL:
						// TODO
						break;
					default:
						break;
					}
				}
			});
		});

		return session;
	}

	/**
	 * Download a plugin to the plugin directory.
	 *
	 * @param gav The plugin coordinate
	 * @return A new {@link CommandFuture}
	 */
	public CommandFuture<Outcome> install(String gav) {
		checkNotNull(gav);
		var session = begin(Outcome.class);

		session.sub(installDependency(gav), outcome -> {
			PluginStore.installPlugin(Environment.LIB.path().resolve(fromCoordinate(gav).filename));
		});

		return session;
	}

	/**
	 * Download a dependency to the library directory.
	 *
	 * @return A new {@link CommandFuture}
	 */
	public CommandFuture<Outcome> installDependency(String gav) {
		checkNotNull(gav);
		var session = begin(Outcome.class);

		Path destination = Environment.LIB.path().resolve(fromCoordinate(gav).filename);
		if (Files.exists(destination))
			// Nothing to do
			return session.success();

		var rq = RQ_ArtifactDownload.newBuilder().setCoordinates(gav).setLocation(false);

		session.request(rq).handle(RS_ArtifactDownload.class, rs -> {
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
		}).handle(Outcome.class, rs -> {
			//
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
