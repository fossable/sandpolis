//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.s7s.core.instance.plugin.PluginStore.PluginStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.s7s.core.foundation.S7SMavenArtifact;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.protocol.Plugin.RQ_DownloadArtifact;
import org.s7s.core.protocol.Plugin.RS_DownloadArtifact;
import org.s7s.core.protocol.Plugin.RS_SyncPlugins;
import org.s7s.core.instance.plugin.Plugin;
import org.s7s.core.instance.plugin.PluginStore;
import org.s7s.core.instance.cmdlet.Cmdlet;

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

		return request(RS_SyncPlugins.class, RS_SyncPlugins.newBuilder()).thenCompose(rs -> {
			return CompletableFuture.allOf(rs.getPluginList().stream().filter(config -> {
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
	public CompletionStage<Void> install(String gav) {
		checkNotNull(gav);

		return installDependency(gav).thenApply(rs -> {
			PluginStore.installPlugin(InstanceContext.PATH_LIB.get().resolve(S7SMavenArtifact.of(gav).filename()));
			return null;
		});
	}

	/**
	 * Download a dependency to the library directory.
	 *
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Void> installDependency(String gav) {
		checkNotNull(gav);

		Path destination = InstanceContext.PATH_LIB.get().resolve(S7SMavenArtifact.of(gav).filename());
		if (Files.exists(destination))
			// Nothing to do
			return CompletableFuture.completedStage(null);

		var rq = RQ_DownloadArtifact.newBuilder().setCoordinates(gav).setLocation(false);

		return request(RS_DownloadArtifact.class, rq).thenCompose(rs -> {
			try {
				switch (rs.getSourceCase()) {
				case BINARY:
					Files.write(destination, rs.getBinary().toByteArray());
					break;
				case COORDINATES:
					try (var out = Files.newOutputStream(destination)) {
						S7SMavenArtifact.of(rs.getCoordinates()).download().transferTo(out);
					}
					break;
				case URL:
					break;
				default:
					throw new RuntimeException();
				}

				// Get any missing dependencies recursively
				// TODO
//				return CompletableFuture.allOf(SoiUtil.getMatrix(destination).getAllDependencies()
//						.map(dep -> installDependency(dep.getCoordinates())).toArray(CompletableFuture[]::new));
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
