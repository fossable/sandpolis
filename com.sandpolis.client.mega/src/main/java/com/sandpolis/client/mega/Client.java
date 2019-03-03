/******************************************************************************
 *                                                                            *
 *                    Copyright 2015 Subterranean Security                    *
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
package com.sandpolis.client.mega;

import static com.sandpolis.core.instance.Environment.EnvPath.DB;
import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.NLIB;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;
import static com.sandpolis.core.net.store.network.NetworkStore.Events.SRV_ESTABLISHED;
import static com.sandpolis.core.net.store.network.NetworkStore.Events.SRV_LOST;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.client.mega.cmd.AuthCmd;
import com.sandpolis.client.mega.cmd.PluginCmd;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.store.artifact.ArtifactStore;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.IPCTasks;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.util.Auth.KeyContainer;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

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
		try {
			SO_CONFIG = MegaConfig.parseFrom(MainDispatch.getMain().getResourceAsStream("/soi/client.bin"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SO_CONFIG!", e);
		}
	}

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis Client"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		MainDispatch.register(BasicTasks::loadConfiguration);
		MainDispatch.register(IPCTasks::checkLocks);
		MainDispatch.register(Client::install);
		MainDispatch.register(Client::loadEnvironment);
		MainDispatch.register(Client::loadStores);
		MainDispatch.register(Client::loadPlugins);
		MainDispatch.register(Client::beginConnectionRoutine);
	}

	/**
	 * Install the client if necessary.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Install client", fatal = true)
	private static TaskOutcome install() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (Environment.JAR == null)
			return task.skipped();

		// Installation mode
		Path base = Paths.get(SO_CONFIG.getExecution().getInstallPathOrDefault(0, "."));// TODO 0
		Path lib = base.resolve("jlib");

		try {
			Files.createDirectories(base);
			Files.createDirectories(lib);
		} catch (IOException e) {
			return task.failure(e);
		}

		try {
			Files.copy(Client.class.getResourceAsStream("/main/main.jar"), base.resolve("client.jar"));
		} catch (Exception e) {
			return task.failure(e);
		}

		ArtifactStore.getDependencies(Instance.CLIENT).forEach(artifact -> {
			String name = ArtifactStore.getArtifactFilename(artifact.getCoordinates());

			try {
				Files.copy(Client.class.getResourceAsStream("/lib/" + name), lib.resolve(name));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		System.exit(0);
		return task.failure();
	}

	/**
	 * Load the runtime environment.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static TaskOutcome loadEnvironment() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Environment.load(DB, TMP, LOG, JLIB, NLIB)) {
			try {
				Environment.setup();
			} catch (RuntimeException e) {
				return task.failure(e);
			}
		}

		return task.success();
	}

	/**
	 * Load static stores.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	public static TaskOutcome loadStores() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		// Load ThreadStore
		ThreadStore.register(Executors.newSingleThreadExecutor(r -> {
			var s = new Thread(r, "SIGNALER");
			s.setDaemon(true);
			return s;
		}), "signaler");
		ThreadStore.register(new NioEventLoopGroup(4), "net.exelet");
		ThreadStore.register(new NioEventLoopGroup(2), "net.connection.outgoing");
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), "net.message.incoming");
		Signaler.init(ThreadStore.get("signaler"));

		// Load NetworkStore
		NetworkStore.init();

		// Load PluginStore
		PluginStore.init(new MemoryListStoreProvider<Plugin>(Plugin.class));

		return task.success();
	}

	/**
	 * Load plugins.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load client plugins")
	private static TaskOutcome loadPlugins() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (Config.getBoolean("no_plugins"))
			return task.skipped();

		try {
			PluginStore.scanPluginDirectory();

			PluginStore.loadPlugins();
		} catch (Exception e) {
			return task.failure(e);
		}

		return task.success();
	}

	/**
	 * Begin the connection routine.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Begin the connection routine", fatal = true)
	private static TaskOutcome beginConnectionRoutine() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		Signaler.register(SRV_ESTABLISHED, () -> {
			var auth = SO_CONFIG.getAuthentication();
			try {
				switch (auth.getAuthOneofCase()) {
				case KEY:
					KeyContainer mech = auth.getKey();
					ReciprocalKeyPair key = new ReciprocalKeyPair(mech.getClient().getVerifier().toByteArray(),
							mech.getClient().getSigner().toByteArray());
					AuthCmd.key(auth.getGroupName(), mech.getId(), key);
					break;
				case PASSWORD:
					AuthCmd.async().password(auth.getPassword().getPassword()).get();
					break;
				default:
					AuthCmd.async().none().get();
					break;
				}

				// Synchronize plugins
				PluginCmd.async().beginSync().get();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		Signaler.register(SRV_LOST, () -> {
			ConnectionStore.connect(SO_CONFIG.getNetwork().getLoopConfig(), new Class[] {});
		});
		Signaler.fire(SRV_LOST);

		return task.success();
	}

	private Client() {
	}
}
