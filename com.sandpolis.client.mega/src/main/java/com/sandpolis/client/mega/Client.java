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
package com.sandpolis.client.mega;

import static com.sandpolis.core.instance.Environment.printEnvironment;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.client.mega.cmd.AuthCmd;
import com.sandpolis.client.mega.exe.ClientExe;
import com.sandpolis.core.cv.cmd.PluginCmd;
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Auth.KeyContainer;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Generator.MegaConfig;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.net.handler.sand5.ReciprocalKeyPair;
import com.sandpolis.core.net.init.ClientChannelInitializer;
import com.sandpolis.core.net.network.NetworkEvents.ServerEstablishedEvent;
import com.sandpolis.core.net.network.NetworkEvents.ServerLostEvent;

import com.google.common.eventbus.Subscribe;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * The entry point for Client instances. This class is responsible for
 * initializing the new instance.
 *
 * @author cilki
 * @since 1.0.0
 */
public final class Client {

	private static final Logger log = LoggerFactory.getLogger(Client.class);

	/**
	 * The configuration included in the instance jar.
	 */
	public static final MegaConfig SO_CONFIG;

	static {
		try (var in = Client.class.getResourceAsStream("/soi/client.bin")) {
			if (in != null) {
				SO_CONFIG = MegaConfig.parseFrom(in);
			} else {
				throw new RuntimeException("Missing SO_CONFIG!");
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SO_CONFIG!", e);
		}
	}

	public static void main(String[] args) {
		printEnvironment(log, "Sandpolis Client");

		register(Client.loadEnvironment);
		register(Client.loadStores);
		register(Client.loadPlugins);
		register(Client.beginConnectionRoutine);
	}

	/**
	 * Load the runtime environment.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	public static final Task loadEnvironment = new Task(outcome -> {

		if (SO_CONFIG.getMemory()) {
			// TODO actually remove paths
			Environment.LIB.set(null);
			Environment.LOG.set(null);
			Environment.PLUGIN.set(null);
			Environment.DB.set(null);
			Environment.GEN.set(null);
			Environment.TMP.set(null);
		} else {
			Environment.LIB.set(Config.PATH_LIB.value().orElse(null)).requireReadable();
			Environment.LOG.set(Config.PATH_LOG.value().orElse(null)).requireWritable();
			Environment.PLUGIN.set(Config.PATH_PLUGIN.value().orElse(null)).requireWritable();
		}
		return outcome.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	public static final Task loadStores = new Task(outcome -> {

		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2).next());
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2).next());
			config.defaults.put("net.connection.loop", new NioEventLoopGroup(2).next());
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
			config.defaults.put("attributes", Executors.newScheduledThreadPool(1));
		});

		PluginStore.init(config -> {
			config.ephemeral();
		});

		StreamStore.init(config -> {
			config.ephemeral();
		});

		ConnectionStore.init(config -> {
			config.ephemeral();
			config.initializer = new ClientChannelInitializer(new Class[] { ClientExe.class });
		});

		NetworkStore.init(config -> {
			config.ephemeral();
		});

		NetworkStore.register(new Object() {
			@Subscribe
			private void onSrvLost(ServerLostEvent event) {
				ConnectionStore.connect(SO_CONFIG.getNetwork().getLoopConfig());
			}

			@Subscribe
			private void onSrvEstablished(ServerEstablishedEvent event) {
				CompletionStage<Outcome> future;
				var auth = SO_CONFIG.getAuthentication();

				switch (auth.getAuthOneofCase()) {
				case KEY:
					KeyContainer mech = auth.getKey();
					ReciprocalKeyPair key = new ReciprocalKeyPair(null,
							//
							mech.getClient().getVerifier().toByteArray(),
							//
							mech.getClient().getSigner().toByteArray());
					future = AuthCmd.async().target(event.get()).key(auth.getGroupName(), mech.getId(), key);
					break;
				case PASSWORD:
					future = AuthCmd.async().target(event.get()).password(auth.getPassword().getPassword());
					break;
				default:
					future = AuthCmd.async().target(event.get()).none();
					break;
				}

				future = future.thenApply(rs -> {
					if (!rs.getResult()) {
						// Close the connection
						ConnectionStore.get(event.get()).ifPresent(sock -> {
							sock.close();
						});
					}
					return rs;
				});

				if (Config.PLUGIN_ENABLED.value().orElse(true) && !SO_CONFIG.getMemory()) {
					future.thenAccept(rs -> {
						if (rs.getResult()) {
							// Synchronize plugins
							PluginCmd.async().synchronize().thenRun(PluginStore::loadPlugins);
						}
					});
				}
			}
		});

		return outcome.success();
	});

	/**
	 * Load plugins.
	 */
	@InitializationTask(name = "Load client plugins")
	public static final Task loadPlugins = new Task(outcome -> {
		if (!Config.PLUGIN_ENABLED.value().orElse(true))
			return outcome.skipped();

		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return outcome.success();
	});

	/**
	 * Begin the connection routine.
	 */
	@InitializationTask(name = "Begin the connection routine", fatal = true)
	public static final Task beginConnectionRoutine = new Task(outcome -> {
		ConnectionStore.connect(SO_CONFIG.getNetwork().getLoopConfig());

		return outcome.success();
	});

	private Client() {
	}

	static {
		MainDispatch.register(Client.class);
	}
}
