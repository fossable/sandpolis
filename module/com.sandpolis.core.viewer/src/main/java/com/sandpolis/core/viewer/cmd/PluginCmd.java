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
package com.sandpolis.core.viewer.cmd;

import static com.google.common.base.Preconditions.checkNotNull;
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
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MsgPlugin.RQ_ArtifactDownload;
import com.sandpolis.core.proto.net.MsgPlugin.RQ_PluginList;
import com.sandpolis.core.proto.net.MsgPlugin.RS_ArtifactDownload;
import com.sandpolis.core.proto.net.MsgPlugin.RS_PluginList;
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
	 * Download a plugin to the plugin directory.
	 *
	 * @param gav The plugin coordinate
	 * @return The command future
	 */
	// Duplicated in com.sandpolis.client.mega.cmd.PluginCmd
	public CommandFuture install(String gav) {
		checkNotNull(gav);
		var session = begin();

		session.sub(installDependency(gav), outcome -> {
			PluginStore.installPlugin(Environment.LIB.path().resolve(fromCoordinate(gav).filename));
		});

		return session;
	}

	/**
	 * Download a dependency to the library directory.
	 *
	 * @return The command future
	 */
	// Duplicated in com.sandpolis.client.mega.cmd.PluginCmd
	public CommandFuture installDependency(String gav) {
		checkNotNull(gav);
		var session = begin();

		Path destination = Environment.LIB.path().resolve(fromCoordinate(gav).filename);
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
	 * List all installed plugins.
	 *
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<RS_PluginList> enumerate() {
		return request(RQ_PluginList.newBuilder());
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
