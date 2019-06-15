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

import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;
import static com.sandpolis.core.net.store.network.NetworkStore.Events.SRV_ESTABLISHED;
import static com.sandpolis.core.net.store.network.NetworkStore.Events.SRV_LOST;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.client.mega.cmd.AuthCmd;
import com.sandpolis.client.mega.cmd.PluginCmd;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.ConfigConstant.plugin;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.util.Auth.KeyContainer;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix.Artifact;
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
			SO_CONFIG = MegaConfig.parseFrom(Client.class.getResourceAsStream("/soi/client.bin"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SO_CONFIG!", e);
		}
	}

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis Client"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		MainDispatch.register(BasicTasks.loadConfiguration);
		MainDispatch.register(IPCTask.load);
		MainDispatch.register(IPCTask.checkLock);
		MainDispatch.register(IPCTask.setLock);
		MainDispatch.register(Client.install);
		MainDispatch.register(Client.loadEnvironment);
		MainDispatch.register(BasicTasks.loadStores);
		MainDispatch.register(Client.loadStores);
		MainDispatch.register(Client.loadPlugins);
		MainDispatch.register(Client.beginConnectionRoutine);
	}

	/**
	 * Install the client if necessary.
	 */
	@InitializationTask(name = "Install client", fatal = true)
	private static final Task install = new Task((task) -> {

		if (Environment.JAR == null)
			return task.skipped();

		// Installation mode
		Path base = Paths.get(SO_CONFIG.getExecution().getInstallPathOrDefault(0, "."));// TODO 0
		Path lib = base.resolve("lib");

		Files.createDirectories(base);
		Files.createDirectories(lib);

		Files.copy(Client.class.getResourceAsStream("/main/main.jar"), base.resolve("client.jar"));

		for (Artifact artifact : Core.SO_MATRIX.getArtifactList()) {
			String name = fromCoordinate(artifact.getCoordinates()).filename;

			Files.copy(Client.class.getResourceAsStream("/lib/" + name), lib.resolve(name));
		}

		System.exit(0);
		return task.success();
	});

	/**
	 * Load the runtime environment.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static final Task loadEnvironment = new Task((task) -> {

		if (!Environment.load(TMP, LOG, LIB))
			Environment.setup();

		return task.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	private static final Task loadStores = new Task((task) -> {

		// Load ThreadStore
		ThreadStore.register(new NioEventLoopGroup(2).next(), net.exelet);
		ThreadStore.register(new NioEventLoopGroup(2).next(), net.connection.outgoing);
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), net.message.incoming);

		// Load NetworkStore
		NetworkStore.init();

		// Load PluginStore
		PluginStore.init(new MemoryListStoreProvider<Plugin>(Plugin.class));

		return task.success();
	});

	/**
	 * Load plugins.
	 */
	@InitializationTask(name = "Load client plugins", condition = plugin.enabled)
	private static final Task loadPlugins = new Task((task) -> {
		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return task.success();
	});

	/**
	 * Begin the connection routine.
	 */
	@InitializationTask(name = "Begin the connection routine", fatal = true)
	private static final Task beginConnectionRoutine = new Task((task) -> {

		Signaler.register(SRV_ESTABLISHED, () -> {
			ResponseFuture<Outcome> future;
			var auth = SO_CONFIG.getAuthentication();

			switch (auth.getAuthOneofCase()) {
			case KEY:
				KeyContainer mech = auth.getKey();
				ReciprocalKeyPair key = new ReciprocalKeyPair(mech.getClient().getVerifier().toByteArray(),
						mech.getClient().getSigner().toByteArray());
				future = AuthCmd.async().key(auth.getGroupName(), mech.getId(), key);
				break;
			case PASSWORD:
				future = AuthCmd.async().password(auth.getPassword().getPassword());
				break;
			default:
				future = AuthCmd.async().none();
				break;
			}

			future.addHandler((Outcome rs) -> {
				// Synchronize plugins
				PluginCmd.async().sync();
			});
		});

		Signaler.register(SRV_LOST, () -> {
			ConnectionStore.connect(SO_CONFIG.getNetwork().getLoopConfig(), new Class[] {});
		});
		Signaler.fire(SRV_LOST);

		return task.success();
	});

	private Client() {
	}

	static {
		MainDispatch.register(Client.class);
	}
}
