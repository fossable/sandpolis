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
package com.sandpolis.server.vanilla;

import static com.sandpolis.core.instance.Environment.printEnvironment;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.store.database.DatabaseStore.DatabaseStore;
import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;
import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;
import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.server.vanilla.store.group.GroupStore.GroupStore;
import static com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStore;
import static com.sandpolis.server.vanilla.store.location.LocationStore.LocationStore;
import static com.sandpolis.server.vanilla.store.server.ServerStore.ServerStore;
import static com.sandpolis.server.vanilla.store.trust.TrustStore.TrustStore;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Auth.PasswordContainer;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Generator.AuthenticationConfig;
import com.sandpolis.core.instance.Generator.ExecutionConfig;
import com.sandpolis.core.instance.Generator.FeatureSet;
import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.instance.Generator.LoopConfig;
import com.sandpolis.core.instance.Generator.MegaConfig;
import com.sandpolis.core.instance.Generator.NetworkConfig;
import com.sandpolis.core.instance.Generator.NetworkTarget;
import com.sandpolis.core.instance.Generator.OutputFormat;
import com.sandpolis.core.instance.Generator.OutputPayload;
import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.ShutdownTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.profile.attribute.Attribute;
import com.sandpolis.core.profile.attribute.Attribute.BooleanAttribute;
import com.sandpolis.core.profile.attribute.Attribute.ByteAttribute;
import com.sandpolis.core.profile.attribute.Attribute.DoubleAttribute;
import com.sandpolis.core.profile.attribute.Attribute.IntegerAttribute;
import com.sandpolis.core.profile.attribute.Attribute.LongAttribute;
import com.sandpolis.core.profile.attribute.Attribute.OsTypeAttribute;
import com.sandpolis.core.profile.attribute.Attribute.StringAttribute;
import com.sandpolis.core.profile.attribute.Collection;
import com.sandpolis.core.profile.attribute.Document;
import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.core.storage.hibernate.HibernateConnection;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;
import com.sandpolis.core.util.Platform.Instance;
import com.sandpolis.core.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.Platform.OsType;
import com.sandpolis.server.vanilla.auth.KeyMechanism;
import com.sandpolis.server.vanilla.auth.PasswordMechanism;
import com.sandpolis.server.vanilla.gen.MegaGen;
import com.sandpolis.server.vanilla.store.group.Group;
import com.sandpolis.server.vanilla.store.listener.Listener;
import com.sandpolis.server.vanilla.store.trust.TrustAnchor;
import com.sandpolis.server.vanilla.store.user.User;

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
		printEnvironment(log, "Sandpolis Server");

		register(Server.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Server.loadEnvironment);
		register(Server.generateCvid);
		register(Server.loadServerStores);
		register(Server.loadPlugins);
		register(Server.installDebugClient);
		register(Server.installAdminUser);
		register(Server.loadListeners);

		register(Server.shutdown);
	}

	/**
	 * Load the Server instance's configuration.
	 */
	@InitializationTask(name = "Load server configuration", fatal = true)
	public static final Task loadConfiguration = new Task(outcome -> {
		Config.DB_PROVIDER.register("hibernate");
		Config.DB_URL.register();
		Config.DB_USERNAME.register();
		Config.DB_PASSWORD.register();

		Config.DEBUG_CLIENT.register(true);

		Config.BANNER_TEXT.register("Welcome to a Sandpolis Server");
		Config.BANNER_IMAGE.register();

		Config.GEOLOCATION_SERVICE.register("ip-api.com");

		Config.PATH_DB.register();
		Config.PATH_GEN.register();

		Config.MESSAGE_TIMEOUT.register(1000);

		return outcome.success();
	});

	/**
	 * Check the runtime environment for fatal errors.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	public static final Task loadEnvironment = new Task(outcome -> {

		Environment.LIB.set(Config.PATH_LIB.value().orElse(null)).requireReadable();
		Environment.DB.set(Config.PATH_DB.value().orElse(null)).requireWritable();
		Environment.GEN.set(Config.PATH_GEN.value().orElse(null)).requireWritable();
		Environment.LOG.set(Config.PATH_LOG.value().orElse(null)).requireWritable();
		Environment.PLUGIN.set(Config.PATH_PLUGIN.value().orElse(null)).requireWritable();
		Environment.TMP.set(Config.PATH_TMP.value().orElse(null)).requireWritable();
		return outcome.success();
	});

	/**
	 * Set a new CVID.
	 */
	@InitializationTask(name = "Generate instance CVID", fatal = true)
	public static final Task generateCvid = new Task(outcome -> {
		Core.setCvid(CvidUtil.cvid(Instance.SERVER));
		return outcome.success();
	});

	/**
	 * Install the administrator user from a credentials file located in the
	 * database directory.
	 */
	@InitializationTask(name = "Install administrator user", fatal = true)
	public static final Task installAdminUser = new Task(outcome -> {
		Path install = Environment.DB.path().resolve("install.txt");
		if (!Files.exists(install))
			return outcome.skipped();

		try {
			var lines = Files.readAllLines(install);
			if (lines.size() != 2)
				return outcome.failure("File format error");

			if (UserStore.count() != 0) {
				return outcome.failure("UserStore not empty");
			}

			String username = lines.get(0);
			String password = lines.get(1);

			UserStore.add(UserConfig.newBuilder().setUsername(username).setPassword(password));
			ListenerStore.add(ListenerConfig.newBuilder().setId(1).setPort(8768).setAddress("0.0.0.0")
					.setOwner(username).setName("Default Listener").setEnabled(true).build());
		} finally {
			try {
				Files.delete(install);
			} catch (IOException e) {
				log.error("Failed to delete install credentials!");
				return outcome.failure(e);
			}
		}

		return outcome.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	public static final Task loadServerStores = new Task(outcome -> {

		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2));
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("server.generator", Executors.newCachedThreadPool());
			config.defaults.put("net.ipc.listener", Executors.newSingleThreadExecutor());
			config.defaults.put("net.ipc.receptor", Executors.newSingleThreadExecutor());
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		NetworkStore.init(config -> {
			config.ephemeral();
			config.cvid = Core.cvid();
		});

		ConnectionStore.init(config -> {
			config.ephemeral();
		});

		StreamStore.init(config -> {
			config.ephemeral();
		});

		PrefStore.init(config -> {
			config.instance = Instance.SERVER;
			config.flavor = InstanceFlavor.VANILLA;
		});

		DatabaseStore.init(config -> {

			// Build connection URL
			String url = String.format("jdbc:h2:%s", Environment.DB.path().resolve("server.db").toUri());

			Configuration conf = new Configuration()
					// Set the H2 database driver
					.setProperty("hibernate.connection.driver_class", "org.h2.Driver")

					// Set the H2 dialect
					.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")

					// Set the credentials
					.setProperty("hibernate.connection.username", "").setProperty("hibernate.connection.password", "")

					// Set the database URL
					.setProperty("hibernate.connection.url", url)

					// Set additional options
					.setProperty("hibernate.connection.shutdown", "true")
					.setProperty("hibernate.globally_quoted_identifiers", "true")
					.setProperty("hibernate.hbm2ddl.auto", "update");

			List.of(Database.class, Listener.class, Group.class, User.class, Profile.class, Document.class,
					Collection.class, Attribute.class, StringAttribute.class, IntegerAttribute.class,
					LongAttribute.class, BooleanAttribute.class, DoubleAttribute.class, ByteAttribute.class,
					OsTypeAttribute.class, Plugin.class, TrustAnchor.class, ReciprocalKeyPair.class, KeyMechanism.class,
					PasswordMechanism.class).forEach(conf::addAnnotatedClass);

			config.main = new Database(url, new HibernateConnection(conf.buildSessionFactory()));
			config.ephemeral();
		});

		UserStore.init(config -> {
			config.persistent(DatabaseStore.main());
		});

		ListenerStore.init(config -> {
			config.persistent(DatabaseStore.main());
		});

		GroupStore.init(config -> {
			config.persistent(DatabaseStore.main());
		});

		ProfileStore.init(config -> {
			// config.persistent(DatabaseStore.main());
			config.ephemeral();
		});

		TrustStore.init(config -> {
			config.persistent(DatabaseStore.main());
		});

		PluginStore.init(config -> {
			config.persistent(DatabaseStore.main());
			config.verifier = TrustStore::verifyPluginCertificate;
		});

		ServerStore.init(config -> {
		});

		LocationStore.init(config -> {
			config.service = Config.GEOLOCATION_SERVICE.value().orElse(null);
			config.key = Config.GEOLOCATION_SERVICE_KEY.value().orElse(null);
			config.cacheExpiration = Duration.ofDays(10);
		});

		return outcome.success();
	});

	@ShutdownTask
	public static final Task shutdown = new Task(outcome -> {
		NetworkStore.close();
		ConnectionStore.close();
		PrefStore.close();
		DatabaseStore.close();
		UserStore.close();
		ListenerStore.close();
		GroupStore.close();
		ProfileStore.close();
		TrustStore.close();
		PluginStore.close();
		ThreadStore.close();

		return outcome.success();
	});

	/**
	 * Load socket listeners.
	 */
	@InitializationTask(name = "Load socket listeners")
	public static final Task loadListeners = new Task(outcome -> {
		ListenerStore.start();

		return outcome.success();
	});

	/**
	 * Load plugins.
	 */
	@InitializationTask(name = "Load server plugins")
	public static final Task loadPlugins = new Task(outcome -> {
		if (!Config.PLUGIN_ENABLED.value().orElse(true))
			return outcome.skipped();

		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return outcome.success();
	});

	/**
	 * Install a debug client on the local machine.
	 */
	@InitializationTask(name = "Install debug client", development = true)
	public static final Task installDebugClient = new Task(outcome -> {
		if (!Config.DEBUG_CLIENT.value().orElse(false))
			return outcome.skipped();

		// Create user and listener
		if (UserStore.get("admin").isEmpty())
			UserStore.add(UserConfig.newBuilder().setUsername("admin").setPassword(CryptoUtil.hash(SHA256, "password"))
					.build());

		if (ListenerStore.get(1L).isEmpty())
			ListenerStore.add(ListenerConfig.newBuilder().setId(1).setPort(8768).setAddress("0.0.0.0").setOwner("admin")
					.setName("test").setEnabled(true).build());

		// Create group
		if (GroupStore.get("1").isEmpty())
			GroupStore.add(GroupConfig.newBuilder().setId("1").setName("test group").setOwner("admin")
					.addPasswordMechanism(PasswordContainer.newBuilder().setPassword("12345")).build());

		// Generate client
		MegaGen generator = MegaGen
				.build(GenConfig.newBuilder().setPayload(OutputPayload.OUTPUT_MEGA).setFormat(OutputFormat.JAR)
						.setMega(MegaConfig.newBuilder().setMemory(false).setFeatures(FeatureSet.newBuilder()
								.addPlugin("com.sandpolis.plugin.desktop").addPlugin("com.sandpolis.plugin.filesys")
								.addPlugin("com.sandpolis.plugin.sysinfo").addPlugin("com.sandpolis.plugin.shell"))
								.setExecution(ExecutionConfig.newBuilder().putInstallPath(OsType.LINUX_VALUE,
										"/home/cilki/.sandpolis"))
								.setAuthentication(AuthenticationConfig.newBuilder()
										.setPassword(PasswordContainer.newBuilder().setPassword("12345")))
								.setNetwork(NetworkConfig.newBuilder().setLoopConfig(
										LoopConfig.newBuilder().setTimeout(5000).setCooldown(5000).addTarget(
												NetworkTarget.newBuilder().setAddress("10.0.1.128").setPort(8768)))))
						.build());
		generator.run();

		if (generator.getReport().getResult()) {
			// Execute
			Runtime.getRuntime()
					.exec(new String[] { "java", "-jar", Environment.GEN.path().resolve("0.jar").toString() });
			return outcome.success();
		} else {
			return outcome.failure(generator.getReport().getComment());
		}
	});

	private Server() {
	}

	static {
		MainDispatch.register(Server.class);
	}

}
