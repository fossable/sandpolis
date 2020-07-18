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
import static com.sandpolis.core.foundation.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;
import static com.sandpolis.core.instance.msg.MsgPlugin.RQ_PluginOperation.PluginOperation.PLUGIN_SYNC;
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.foundation.soi.SoiUtil;
import com.sandpolis.core.foundation.util.ArtifactUtil;
import com.sandpolis.core.foundation.util.NetUtil;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.msg.MsgPlugin.RQ_ArtifactDownload;
import com.sandpolis.core.instance.msg.MsgPlugin.RQ_PluginOperation;
import com.sandpolis.core.instance.msg.MsgPlugin.RS_ArtifactDownload;
import com.sandpolis.core.instance.msg.MsgPlugin.RS_PluginSync;
import com.sandpolis.core.instance.plugin.Plugin;
import com.sandpolis.core.instance.plugin.PluginStore;
import com.sandpolis.core.net.command.Cmdlet;

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
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Void> synchronize() {

		return request(RS_PluginSync.class, RQ_PluginOperation.newBuilder().setOperation(PLUGIN_SYNC))
				.thenCompose(rs -> {
					return CompletableFuture.allOf(rs.getPluginConfigList().stream().filter(config -> {
						Optional<Plugin> plugin = PluginStore.getByPackageId(config.getPackageId());
						if (plugin.isEmpty())
							return true;

						// Check versions
						return !plugin.get().getVersion().equals(config.getVersion());
					}).map(config -> {
						if (PluginStore.getByPackageId(config.getPackageId()).isEmpty()) {
							switch (config.getSourceCase()) {
							case PLUGIN_BINARY:
								// TODO
								break;
							case PLUGIN_COORDINATES:
								return install(config.getPluginCoordinates());
							case PLUGIN_URL:
								// TODO
								break;
							default:
								break;
							}
						}
						return CompletableFuture.completedFuture(null);
					}).toArray(CompletableFuture[]::new));
				});
	}

	/**
	 * Download a plugin to the plugin directory.
	 *
	 * @param gav The plugin coordinate
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> install(String gav) {
		checkNotNull(gav);

		return installDependency(gav).thenApply(outcome -> {
			PluginStore.installPlugin(Environment.LIB.path().resolve(fromCoordinate(gav).filename));
			return Outcome.newBuilder().setResult(true).build();
		});
	}

	/**
	 * Download a dependency to the library directory.
	 *
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Void> installDependency(String gav) {
		checkNotNull(gav);

		Path destination = Environment.LIB.path().resolve(fromCoordinate(gav).filename);
		if (Files.exists(destination))
			// Nothing to do
			return CompletableFuture.completedStage(null);

		var rq = RQ_ArtifactDownload.newBuilder().setCoordinates(gav).setLocation(false);

		return request(RS_ArtifactDownload.class, rq).thenCompose(rs -> {
			try {
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
					throw new RuntimeException();
				}

				// Get any missing dependencies recursively
				return CompletableFuture.allOf(SoiUtil.getMatrix(destination).getAllDependencies()
						.map(dep -> installDependency(dep.getCoordinates())).toArray(CompletableFuture[]::new));
			} catch (NoSuchFileException e) {
				// This dependency does not have a soi/matrix.bin (skip it)
			} catch (IOException e) {
				return CompletableFuture.failedStage(e);
			}

			return CompletableFuture.failedStage(null);
		});
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
