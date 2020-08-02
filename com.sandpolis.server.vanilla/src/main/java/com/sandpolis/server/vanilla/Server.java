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
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.exelet.ExeletStore.ExeletStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;
import static com.sandpolis.server.vanilla.store.group.GroupStore.GroupStore;
import static com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStore;
import static com.sandpolis.server.vanilla.store.location.LocationStore.LocationStore;
import static com.sandpolis.server.vanilla.store.server.ServerStore.ServerStore;
import static com.sandpolis.server.vanilla.store.trust.TrustStore.TrustStore;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.instance.Auth.PasswordContainer;
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
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.server.vanilla.exe.AuthExe;
import com.sandpolis.server.vanilla.exe.GenExe;
import com.sandpolis.server.vanilla.exe.GroupExe;
import com.sandpolis.server.vanilla.exe.ListenerExe;
import com.sandpolis.server.vanilla.exe.LoginExe;
import com.sandpolis.server.vanilla.exe.PluginExe;
import com.sandpolis.server.vanilla.exe.ServerExe;
import com.sandpolis.server.vanilla.exe.StreamExe;
import com.sandpolis.server.vanilla.exe.UserExe;
import com.sandpolis.server.vanilla.gen.MegaGen;
import com.sandpolis.server.vanilla.hibernate.HibernateStoreProviderFactory;

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
		register(Server.loadEnvironment);
		register(Server.generateCvid);
		register(Server.loadServerStores);
		register(Server.loadPlugins);
		register(Server.firstTimeSetup);
		register(Server.installDebugClient);
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

		Config.PLUGIN_ENABLED.register(true);

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
		Core.setCvid(CvidUtil.cvid(InstanceType.SERVER));
		return outcome.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	public static final Task loadServerStores = new Task(outcome -> {

		Configuration conf = new Configuration()
				//
				.setProperty("hibernate.ogm.datastore.create_database", "true");

		List.of(com.sandpolis.core.instance.data.Document.class, com.sandpolis.core.instance.data.Collection.class,
				com.sandpolis.core.instance.data.Attribute.class,
				com.sandpolis.core.instance.data.StringAttribute.class,
				com.sandpolis.core.instance.data.IntegerAttribute.class,
				com.sandpolis.core.instance.data.LongAttribute.class,
				com.sandpolis.core.instance.data.BooleanAttribute.class,
				com.sandpolis.core.instance.data.DoubleAttribute.class,
				com.sandpolis.core.instance.data.OsTypeAttribute.class,
				com.sandpolis.core.instance.data.X509CertificateAttribute.class,
				com.sandpolis.core.instance.data.InstanceFlavorAttribute.class,
				com.sandpolis.core.instance.data.InstanceTypeAttribute.class,
				com.sandpolis.server.vanilla.hibernate.HibernateStoreProvider.class,
				com.sandpolis.server.vanilla.hibernate.HibernateStoreProviderMetadata.class)
				.forEach(conf::addAnnotatedClass);

		StoreProviderFactory providerFactory;
		switch (Config.STORAGE_PROVIDER.value().orElse("mongodb")) {
		case "mongodb":
			conf
					//
					.setProperty("hibernate.ogm.datastore.provider", "mongodb")
					//
					.setProperty("hibernate.ogm.datastore.database",
							Config.MONGODB_DATABASE.value().orElse("Sandpolis"))
					//
					.setProperty("hibernate.ogm.datastore.host", Config.MONGODB_HOST.value().orElse("127.0.0.1"))
					.setProperty("hibernate.ogm.datastore.username", Config.MONGODB_USER.value().orElse(""))
					.setProperty("hibernate.ogm.datastore.password", Config.MONGODB_PASSWORD.value().orElse(""));
			providerFactory = new HibernateStoreProviderFactory(conf);
			break;
		case "infinispan_embedded":
			providerFactory = null;
			break;
		case "ephemeral":
			providerFactory = null;
			break;
		default:
			providerFactory = null;
			break;
		}

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

		ExeletStore.init(config -> {
			config.exelets = List.of(AuthExe.class, GenExe.class, GroupExe.class, ListenerExe.class, LoginExe.class,
					ServerExe.class, UserExe.class, PluginExe.class, StreamExe.class);
		});

		StreamStore.init(config -> {
			config.ephemeral();
		});

		PrefStore.init(config -> {
			config.instance = InstanceType.SERVER;
			config.flavor = InstanceFlavor.VANILLA;
		});

		UserStore.init(config -> {
			if (providerFactory != null)
				config.persistent(providerFactory);
			else
				config.ephemeral();
		});

		ListenerStore.init(config -> {
			if (providerFactory != null)
				config.persistent(providerFactory);
			else
				config.ephemeral();
		});

		GroupStore.init(config -> {
			if (providerFactory != null)
				config.persistent(providerFactory);
			else
				config.ephemeral();
		});

		ProfileStore.init(config -> {
			if (providerFactory != null)
				config.persistent(providerFactory);
			else
				config.ephemeral();
		});

		TrustStore.init(config -> {
			if (providerFactory != null)
				config.persistent(providerFactory);
			else
				config.ephemeral();
		});

		PluginStore.init(config -> {
			config.verifier = TrustStore::verifyPluginCertificate;
			if (providerFactory != null)
				config.persistent(providerFactory);
			else
				config.ephemeral();
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

	@InitializationTask(name = "First time initialization")
	public static final Task firstTimeSetup = new Task(outcome -> {
		boolean skipped = true;

		// Setup default users
		if (UserStore.getMetadata().getInitCount() == 1) {
			UserStore.create(UserConfig.newBuilder().setUsername("admin").setPassword("password").build());
			skipped = false;
		}

		// Setup default listeners
		if (ListenerStore.getMetadata().getInitCount() == 1) {
			ListenerStore.create(ListenerConfig.newBuilder().setPort(8768).setAddress("0.0.0.0").setOwner("admin")
					.setName("Default Listener").setEnabled(true).build());
			skipped = false;
		}

		// Setup default groups
		if (GroupStore.getMetadata().getInitCount() == 1) {
			GroupStore.create(GroupConfig.newBuilder().setName("Default Authentication Group").setOwner("admin")
					.addPasswordMechanism(PasswordContainer.newBuilder().setPassword("12345")).build());
			skipped = false;
		}

		if (skipped)
			return outcome.skipped();

		return outcome.success();
	});

	/**
	 * Install a debug client on the local machine.
	 */
	@InitializationTask(name = "Install debug client", development = true)
	public static final Task installDebugClient = new Task(outcome -> {
		if (!Config.DEBUG_CLIENT.value().orElse(false))
			return outcome.skipped();

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
								.setNetwork(
										NetworkConfig.newBuilder()
												.setLoopConfig(LoopConfig.newBuilder().setTimeout(5000)
														.setCooldown(5000)
														.addTarget(NetworkTarget.newBuilder()
																.setAddress("host.docker.internal").setPort(8768)))))
						.build());
		generator.run();

		if (generator.getReport().getResult()) {
			// Execute locally
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
