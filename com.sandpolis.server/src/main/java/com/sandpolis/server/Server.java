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
package com.sandpolis.server;

import static com.sandpolis.core.instance.Environment.EnvPath.DB;
import static com.sandpolis.core.instance.Environment.EnvPath.GEN;
import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.attribute.AttributeDomain;
import com.sandpolis.core.attribute.AttributeGroup;
import com.sandpolis.core.attribute.AttributeNode;
import com.sandpolis.core.attribute.UntrackedAttribute;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.storage.database.DatabaseFactory;
import com.sandpolis.core.instance.store.database.DatabaseStore;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.IPCTasks;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.profile.ProfileStore;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Generator.LoopConfig;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Generator.NetworkConfig;
import com.sandpolis.core.proto.util.Generator.NetworkTarget;
import com.sandpolis.core.proto.util.Generator.OutputFormat;
import com.sandpolis.core.proto.util.Generator.OutputPayload;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;
import com.sandpolis.core.util.IDUtil;
import com.sandpolis.server.auth.KeyMechanism;
import com.sandpolis.server.auth.PasswordMechanism;
import com.sandpolis.server.gen.generator.MegaGen;
import com.sandpolis.server.store.group.Group;
import com.sandpolis.server.store.group.GroupStore;
import com.sandpolis.server.store.listener.Listener;
import com.sandpolis.server.store.listener.ListenerStore;
import com.sandpolis.server.store.trust.TrustAnchor;
import com.sandpolis.server.store.trust.TrustStore;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * The entry point for Server instances. This class is responsible for
 * initializing the new instance.
 * 
 * @author cilki
 * @since 1.0.0
 */
public final class Server {
	private Server() {
	}

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis Server"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		MainDispatch.register(BasicTasks::loadConfiguration);
		MainDispatch.register(Server::loadConfiguration);
		MainDispatch.register(IPCTasks::checkLocks);
		MainDispatch.register(Server::loadEnvironment);
		MainDispatch.register(Server::loadServerStores);
		MainDispatch.register(Server::loadPlugins);
		MainDispatch.register(Server::installDebugClient);
		MainDispatch.register(Server::loadListeners);
		MainDispatch.register(Server::post);

	}

	/**
	 * Load the Server instance's configuration.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load server configuration", fatal = true)
	public static TaskOutcome loadConfiguration() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		Config.register("debug_client", true);

		Config.register("banner.text", "Welcome to a Sandpolis Server");
		Config.register("banner.image.path", null);

		return task.success();
	}

	/**
	 * Check the runtime environment for fatal errors.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static TaskOutcome loadEnvironment() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Environment.load(DB.setDefault(Config.get("path.db")), GEN.setDefault(Config.get("path.gen")),
				LOG.setDefault(Config.get("path.log")), TMP, LIB)) {
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
	public static TaskOutcome loadServerStores() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		// Load ThreadStore
		ThreadStore.register(Executors.newCachedThreadPool(r -> {
			var s = new Thread(r, "SIGNALER");
			s.setDaemon(true);
			return s;
		}), "signaler");
		ThreadStore.register(new NioEventLoopGroup(4), "net.exelet");
		ThreadStore.register(new NioEventLoopGroup(2), "net.connection.outgoing");
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), "net.message.incoming");
		ThreadStore.register(Executors.newSingleThreadExecutor(), "generator");
		Signaler.init(ThreadStore.get("signaler"));

		// Load NetworkStore and choose a new CVID
		Core.setCvid(IDUtil.CVID.cvid(Instance.SERVER));
		NetworkStore.updateCvid(Core.cvid());

		// Load PrefStore
		PrefStore.load(Server.class);

		// Load DatabaseStore
		try {
			if (!Config.has("db.url")) {
				DatabaseStore.load(DatabaseFactory.create("h2", Environment.get(DB).resolve("server.db").toFile()),
						ORM_CLASSES);
			} else {
				DatabaseStore.load(DatabaseFactory.create(Config.get("db.url"), Config.get("db.username"),
						Config.get("db.password")), ORM_CLASSES);
			}
		} catch (URISyntaxException | IOException e) {
			return task.failure(e);
		}

		// Load UserStore
		UserStore.load(DatabaseStore.main());

		// Load ListenerStore
		ListenerStore.load(DatabaseStore.main());

		// Load GroupStore
		GroupStore.load(DatabaseStore.main());

		// Load ProfileStore
		ProfileStore.load(DatabaseStore.main());

		// Load TrustStore
		TrustStore.load(DatabaseStore.main());

		// Load PluginStore
		PluginStore.load(DatabaseStore.main());
		PluginStore.setCertVerifier(TrustStore::verifyPluginCertificate);

		return task.success();
	}

	/**
	 * Load socket listeners.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load socket listeners")
	private static TaskOutcome loadListeners() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		try {
			// TODO debug listener
			ListenerStore.start();
		} catch (Exception e) {
			return task.failure(e);
		}
		return task.complete(true);
	}

	/**
	 * Load plugins.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load server plugins")
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
	 * Install a debug client on the local machine.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Install debug client", debug = true)
	private static TaskOutcome installDebugClient() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Config.getBoolean("debug_client"))
			return task.skipped();

		try {
			// Create user and listener
			UserStore.add(UserConfig.newBuilder().setUsername("admin").setPassword("password").build());

			ListenerStore.add(ListenerConfig.newBuilder().setPort(10101).setAddress("0.0.0.0").setOwner("admin")
					.setName("test").setEnabled(true).build());

			// Create group
			GroupStore.add(GroupConfig.newBuilder().setId("1").setName("test group").setOwner("admin").build());

			// Generate client
			new MegaGen(GenConfig.newBuilder().setPayload(OutputPayload.OUTPUT_MEGA).setFormat(OutputFormat.JAR)
					.setMega(MegaConfig.newBuilder().setNetwork(NetworkConfig.newBuilder()
							.setLoopConfig(LoopConfig.newBuilder().setTimeout(1000).setMaxTimeout(1000)
									.addTarget(NetworkTarget.newBuilder().setAddress("127.0.0.1").setPort(10101)))))
					.build()).generate();

		} catch (Exception e) {
			return task.failure(e);
		}

		return task.success();
	}

	/**
	 * Perform a self-test.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Power-on self test", fatal = true, debug = true)
	private static TaskOutcome post() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Config.getBoolean("post"))
			return task.skipped();

		return task.complete(Post.smokeTest());
	}

	/**
	 * A list of classes that are managed by the ORM for this instance.
	 */
	private static final Class<?>[] ORM_CLASSES = new Class<?>[] { Database.class, Listener.class, Group.class,
			ReciprocalKeyPair.class, KeyMechanism.class, PasswordMechanism.class, User.class, Profile.class,
			AttributeNode.class, AttributeGroup.class, AttributeDomain.class, UntrackedAttribute.class, Plugin.class,
			TrustAnchor.class };

}
