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
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.util.CryptoUtil.SHA256;

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
import com.sandpolis.core.instance.ConfigConstant.path;
import com.sandpolis.core.instance.ConfigConstant.plugin;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.storage.database.DatabaseFactory;
import com.sandpolis.core.instance.store.database.DatabaseStore;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.task.IPCTask;
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
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;
import com.sandpolis.core.util.IDUtil;
import com.sandpolis.server.ConfigConstant.server;
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

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis Server"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		register(BasicTasks.loadConfiguration);
		register(Server.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Server.loadEnvironment);
		register(BasicTasks.loadStores);
		register(Server.loadServerStores);
		register(Server.loadPlugins);
		register(Server.installDebugClient);
		register(Server.loadListeners);
		register(Server.post);

	}

	/**
	 * Load the Server instance's configuration.
	 */
	@InitializationTask(name = "Load server configuration", fatal = true)
	private static final Task loadConfiguration = new Task((task) -> {
		Config.register(server.db.provider, "hibernate");
		Config.register(server.db.url);
		Config.register(server.db.username);
		Config.register(server.db.password);

		Config.register(server.debug_client, true);

		Config.register(server.banner.text, "Welcome to a Sandpolis Server");
		Config.register(server.banner.image);

		return task.success();
	});

	/**
	 * Check the runtime environment for fatal errors.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static final Task loadEnvironment = new Task((task) -> {

		if (!Environment.load(DB.setDefault(Config.get(server.path.db)), GEN.setDefault(Config.get(server.path.gen)),
				LOG.setDefault(Config.get(path.log)), TMP, LIB)) {
			Environment.setup();
		}

		return task.success();
	});

	/**
	 * A list of classes that are managed by the ORM for this instance.
	 */
	private static final Class<?>[] ORM_CLASSES = new Class<?>[] { Database.class, Listener.class, Group.class,
			ReciprocalKeyPair.class, KeyMechanism.class, PasswordMechanism.class, User.class, Profile.class,
			AttributeNode.class, AttributeGroup.class, AttributeDomain.class, UntrackedAttribute.class, Plugin.class,
			TrustAnchor.class };

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	private static final Task loadServerStores = new Task((task) -> {

		// Load ThreadStore
		ThreadStore.register(new NioEventLoopGroup(2), net.exelet);
		ThreadStore.register(new NioEventLoopGroup(2), net.connection.outgoing);
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), net.message.incoming);
		ThreadStore.register(Executors.newCachedThreadPool(), PoolConstant.server.generator);

		// Load NetworkStore and choose a new CVID
		Core.setCvid(IDUtil.CVID.cvid(Instance.SERVER));
		NetworkStore.updateCvid(Core.cvid());

		// Load PrefStore
		PrefStore.load(Core.INSTANCE, Core.FLAVOR);

		// Load DatabaseStore
		if (!Config.has(server.db.url)) {
			DatabaseStore.load(DatabaseFactory.create("h2", Environment.get(DB).resolve("server.db").toFile()),
					ORM_CLASSES);
		} else {
			DatabaseStore.load(DatabaseFactory.create(Config.get(server.db.url), Config.get(server.db.username),
					Config.get(server.db.password)), ORM_CLASSES);
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
	});

	/**
	 * Load socket listeners.
	 */
	@InitializationTask(name = "Load socket listeners")
	private static final Task loadListeners = new Task((task) -> {
		// TODO debug listener
		ListenerStore.start();

		return task.success();
	});

	/**
	 * Load plugins.
	 */
	@InitializationTask(name = "Load server plugins", condition = plugin.enabled)
	private static final Task loadPlugins = new Task((task) -> {
		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return task.success();
	});

	/**
	 * Install a debug client on the local machine.
	 */
	@InitializationTask(name = "Install debug client", debug = true, condition = server.debug_client)
	private static final Task installDebugClient = new Task((task) -> {

		// Create user and listener
		UserStore.add(
				UserConfig.newBuilder().setUsername("admin").setPassword(CryptoUtil.hash(SHA256, "password")).build());

		ListenerStore.add(ListenerConfig.newBuilder().setPort(10101).setAddress("0.0.0.0").setOwner("admin")
				.setName("test").setEnabled(true).build());

		// Create group
		GroupStore.add(GroupConfig.newBuilder().setId("1").setName("test group").setOwner("admin").build());

		// Generate client
		new MegaGen(
				GenConfig.newBuilder().setPayload(OutputPayload.OUTPUT_MEGA).setFormat(OutputFormat.JAR)
						.setMega(MegaConfig.newBuilder().setNetwork(NetworkConfig.newBuilder()
								.setLoopConfig(LoopConfig.newBuilder().setTimeout(1000).setMaxTimeout(1000)
										.addTarget(NetworkTarget.newBuilder().setAddress("127.0.0.1").setPort(10101)))))
						.build()).generate();

		return task.success();
	});

	/**
	 * Perform a self-test.
	 */
	@InitializationTask(name = "Power-on self test", debug = true)
	private static final Task post = new Task((task) -> {
		return task.complete(Post.smokeTest());
	});

	private Server() {
	}

	static {
		MainDispatch.register(Server.class);
	}

}
