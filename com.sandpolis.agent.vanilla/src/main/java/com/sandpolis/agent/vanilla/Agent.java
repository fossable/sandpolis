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
package com.sandpolis.agent.vanilla;

import static com.sandpolis.core.instance.Environment.printEnvironment;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.instance.state.STStore.STStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.exelet.ExeletStore.ExeletStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.agent.vanilla.cmd.AuthCmd;
import com.sandpolis.agent.vanilla.exe.AgentExe;
import com.sandpolis.core.clientagent.cmd.PluginCmd;
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Group.AgentConfig;
import com.sandpolis.core.instance.Group.AgentConfig.LoopConfig;
import com.sandpolis.core.instance.Group.AgentConfig.NetworkTarget;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.profile.Profile;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;
import com.sandpolis.core.instance.state.vst.VirtCollection;
import com.sandpolis.core.net.network.NetworkEvents.ServerEstablishedEvent;
import com.sandpolis.core.net.network.NetworkEvents.ServerLostEvent;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * {@link Agent} is responsible for initializing the instance.
 *
 * @since 1.0.0
 */
public final class Agent {

	private static final Logger log = LoggerFactory.getLogger(Agent.class);

	/**
	 * The configuration included in the instance jar.
	 */
	public static final AgentConfig SO_CONFIG;

	static {
		try (var in = Agent.class.getResourceAsStream("/soi/agent.bin")) {
			if (in != null) {
				SO_CONFIG = AgentConfig.parseFrom(in);
			} else {
				// Set debug configuration
				SO_CONFIG = AgentConfig.newBuilder().setMemory(false)

						// Add standard plugins
						.addPlugin("com.sandpolis.plugin.desktop").addPlugin("com.sandpolis.plugin.filesys")
						.addPlugin("com.sandpolis.plugin.sysinfo").addPlugin("com.sandpolis.plugin.shell")

						// Add installation path
						.putInstallPath(OsType.LINUX_VALUE, "/home/cilki/.sandpolis")

						// Add connection loop configuration
						.setLoopConfig(LoopConfig.newBuilder().setTimeout(5000).setCooldown(5000)
								.addTarget(NetworkTarget.newBuilder().setAddress("172.17.0.1").setPort(8768)))
						.build();
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SO_CONFIG!", e);
		}
	}

	public static void main(String[] args) {
		printEnvironment(log, "Sandpolis Agent");

		register(Agent.loadEnvironment);
		register(Agent.loadStores);
		register(Agent.loadPlugins);
		register(Agent.beginConnectionRoutine);
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
			config.defaults.put("net.exelet", new NioEventLoopGroup(2).next());
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2).next());
			config.defaults.put("net.connection.loop", new NioEventLoopGroup(2).next());
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
			config.defaults.put("attributes", Executors.newScheduledThreadPool(1));
		});

		STStore.init(config -> {
			config.concurrency = 1;
			config.root = new EphemeralDocument();
		});

		ProfileStore.init(config -> {
			config.collection = new VirtCollection<Profile>(STStore.root(), Profile::new);
		});

		PluginStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().plugin();
		});

		StreamStore.init(config -> {
		});

		ExeletStore.init(config -> {
			config.exelets = List.of(AgentExe.class);
		});

		ConnectionStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().connection();
		});

		NetworkStore.init(config -> {
		});

		NetworkStore.register(new Object() {
			@Subscribe
			private void onSrvLost(ServerLostEvent event) {
				ConnectionStore.connect(SO_CONFIG.getLoopConfig());
			}

			@Subscribe
			private void onSrvEstablished(ServerEstablishedEvent event) {
				CompletionStage<Outcome> future;

				switch (SO_CONFIG.getAuthenticationCase()) {
				case PASSWORD:
					future = AuthCmd.async().target(event.get()).password(SO_CONFIG.getPassword().getPassword());
					break;
				default:
					future = AuthCmd.async().target(event.get()).none();
					break;
				}

				future = future.thenApply(rs -> {
					if (!rs.getResult()) {
						// Close the connection
						ConnectionStore.getByCvid(event.get()).ifPresent(sock -> {
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
	@InitializationTask(name = "Load plugins")
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
		ConnectionStore.connect(SO_CONFIG.getLoopConfig()).future().addListener(future -> {
			if (!future.isSuccess()) {
				log.error("Connection loop failed to start", future.cause());
			}
		});

		return outcome.success();
	});

	private Agent() {
	}

	static {
		MainDispatch.register(Agent.class);
	}
}
