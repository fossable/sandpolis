/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.client.mega;

import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.client.mega.cmd.AuthCmd;
import com.sandpolis.client.mega.cmd.PluginCmd;
import com.sandpolis.client.mega.exe.ClientExe;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.util.PlatformUtil;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.init.ClientChannelInitializer;
import com.sandpolis.core.net.store.network.NetworkStoreEvents.ServerEstablishedEvent;
import com.sandpolis.core.net.store.network.NetworkStoreEvents.ServerLostEvent;
import com.sandpolis.core.proto.util.Auth.KeyContainer;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.soi.Dep;
import com.sandpolis.core.util.ArtifactUtil;
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

		register(BasicTasks.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Client.install);
		register(Client.loadEnvironment);
		register(Client.loadStores);
		register(Client.loadPlugins);
		register(Client.beginConnectionRoutine);
	}

	/**
	 * Install the client if necessary.
	 */
	@InitializationTask(name = "Install client", fatal = true)
	public static final Task install = new Task((task) -> {

		if (SO_CONFIG.getMemory())
			// Memory-only installation
			return task.skipped();

		Path base = Paths.get(SO_CONFIG.getExecution().getInstallPathOrDefault(PlatformUtil.OS_TYPE.getNumber(),
				System.getProperty("user.home") + "/.sandpolis"));
		Path lib = base.resolve("lib");

		if (Environment.JAR.getParent().equals(lib))
			// Already installed
			return task.skipped();

		log.debug("Selected base directory: {}", base);

		try {
			Files.createDirectories(base);
			Files.createDirectories(lib);
		} catch (IOException e) {
			// Force install if enabled
			// TODO
			e.printStackTrace();
		}

		try {
			// TODO remove nested libraries
			log.debug("Installing client binary from: {} into: {}", Environment.JAR, lib.toAbsolutePath());
			Files.copy(Environment.JAR, lib.resolve("sandpolis-client-5.1.1.jar"), REPLACE_EXISTING);

		} catch (IOException e) {
			// Force install if enabled
			// TODO
			e.printStackTrace();
		}

		// Install dependencies
		new Dep(Core.SO_MATRIX, Core.SO_MATRIX.getArtifact(0)).getAllDependencies().forEach(dep -> {
			String name = fromCoordinate(dep.getCoordinates()).filename;

			try {
				var in = Client.class.getResourceAsStream("/lib/" + name);
				if (in != null)
					Files.copy(in, lib.resolve(name), REPLACE_EXISTING);
				else
					ArtifactUtil.download(lib.resolve(name), dep.getCoordinates());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		// TODO regular launch
		var cmd = new String[] { "screen", "-S", "com.sandpolis.client.mega", "-X", "stuff",
				"clear && java -Djava.system.class.loader=com.github.cilki.compact.CompactClassLoader -jar "
						+ lib.resolve("sandpolis-client-5.1.1.jar").toAbsolutePath().toString() + "\n" };
		Runtime.getRuntime().exec(cmd);

		System.exit(0);
		return task.success();
	});

	/**
	 * Load the runtime environment.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	public static final Task loadEnvironment = new Task((task) -> {

		if (!Environment.load(TMP, LOG, LIB))
			Environment.setup();

		return task.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	public static final Task loadStores = new Task((task) -> {

		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2).next());
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2).next());
			config.defaults.put("temploop", new NioEventLoopGroup(2).next());
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		ConnectionStore.init(config -> {
			config.ephemeral();
		});

		NetworkStore.init(config -> {
			config.ephemeral();
		});

		PluginStore.init(config -> {
			config.ephemeral();
		});

		// Register the subscribers in this class by building a temporary instance
		var client = new Client();
		NetworkStore.register(client);

		return task.success();
	});

	/**
	 * Load plugins.
	 */
	@InitializationTask(name = "Load client plugins", condition = "plugin.enabled")
	public static final Task loadPlugins = new Task((task) -> {
		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return task.success();
	});

	/**
	 * Begin the connection routine.
	 */
	@InitializationTask(name = "Begin the connection routine", fatal = true)
	public static final Task beginConnectionRoutine = new Task((task) -> {
		// Temporary
		ClientChannelInitializer.setExelets(new Class[] { ClientExe.class });

		ConnectionStore.connect(SO_CONFIG.getNetwork().getLoopConfig());
		return task.success();
	});

	@Subscribe
	private void onSrvLost(ServerLostEvent event) {
		ConnectionStore.connect(SO_CONFIG.getNetwork().getLoopConfig());
	}

	@Subscribe
	private void onSrvEstablished(ServerEstablishedEvent event) {
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

		if (Config.getBoolean("plugin.enabled") && !SO_CONFIG.getMemory()) {
			future.addHandler((Outcome rs) -> {
				// Synchronize plugins
				PluginCmd.async().sync().sync();
				PluginStore.loadPlugins();
			});
		}
	}

	private Client() {
	}

	static {
		MainDispatch.register(Client.class);
	}
}
