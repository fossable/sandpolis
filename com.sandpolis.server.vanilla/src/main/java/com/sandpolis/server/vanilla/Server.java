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
import static com.sandpolis.core.instance.state.STStore.STStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.exelet.ExeletStore.ExeletStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;
import static com.sandpolis.core.server.banner.BannerStore.BannerStore;
import static com.sandpolis.core.server.group.GroupStore.GroupStore;
import static com.sandpolis.core.server.listener.ListenerStore.ListenerStore;
import static com.sandpolis.core.server.location.LocationStore.LocationStore;
import static com.sandpolis.core.server.trust.TrustStore.TrustStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.ShutdownTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.server.auth.AuthExe;
import com.sandpolis.core.server.auth.LoginExe;
import com.sandpolis.core.server.banner.BannerExe;
import com.sandpolis.core.server.generator.GeneratorExe;
import com.sandpolis.core.server.group.GroupExe;
import com.sandpolis.core.server.listener.ListenerExe;
import com.sandpolis.core.server.plugin.PluginExe;
import com.sandpolis.core.server.state.HibernateDocument;
import com.sandpolis.core.server.state.STExe;
import com.sandpolis.core.server.stream.StreamExe;
import com.sandpolis.core.server.user.UserExe;

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

		Config.DEBUG_AGENT.register(true);

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

		List.of(com.sandpolis.core.server.state.HibernateDocument.class,
				com.sandpolis.core.server.state.HibernateAttribute.class,
				com.sandpolis.core.server.state.StringAttributeValue.class,
				com.sandpolis.core.server.state.IntegerAttributeValue.class,
				com.sandpolis.core.server.state.LongAttributeValue.class,
				com.sandpolis.core.server.state.BooleanAttributeValue.class,
				com.sandpolis.core.server.state.DoubleAttributeValue.class,
				com.sandpolis.core.server.state.OsTypeAttributeValue.class,
				com.sandpolis.core.server.state.X509CertificateAttributeValue.class,
				com.sandpolis.core.server.state.InstanceFlavorAttributeValue.class,
				com.sandpolis.core.server.state.InstanceTypeAttributeValue.class,
				com.sandpolis.core.server.hibernate.HibernateCollectionMetadata.class).forEach(conf::addAnnotatedClass);

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

			var em = conf.buildSessionFactory().createEntityManager();
			STStore.init(config -> {
				config.concurrency = 2;
				config.root = em.find(HibernateDocument.class, "st");
				if (config.root == null) {
					config.root = new HibernateDocument(null, AbsoluteOid.ROOT);

					em.getTransaction().begin();
					em.persist(config.root);
					em.flush();
					em.getTransaction().commit();
				}

				// TODO set em
			});

			break;
		case "infinispan_embedded":
			break;
		case "ephemeral":
			STStore.init(config -> {
				config.concurrency = 2;
				config.root = new EphemeralDocument();
			});
			break;
		default:
			break;
		}

		ProfileStore.init(config -> {

		});

		ThreadStore.init(config -> {
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2));
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("server.generator", Executors.newCachedThreadPool());
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		NetworkStore.init(config -> {
			config.cvid = Core.cvid();
		});

		ConnectionStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().connection();
		});

		ExeletStore.init(config -> {
			config.exelets = List.of(AuthExe.class, GeneratorExe.class, GroupExe.class, ListenerExe.class,
					LoginExe.class, BannerExe.class, UserExe.class, PluginExe.class, StreamExe.class, STExe.class);
		});

		StreamStore.init(config -> {
		});

		PrefStore.init(config -> {
			config.instance = InstanceType.SERVER;
			config.flavor = InstanceFlavor.VANILLA;
		});

		BannerStore.init(config -> {
		});

		UserStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().server().user();
		});

		ListenerStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().server().listener();
		});

		GroupStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().server().group();
		});

		TrustStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().server().trustanchor();
		});

		PluginStore.init(config -> {
			config.verifier = TrustStore::verifyPluginCertificate;
			config.collection = ProfileStore.getByUuid(Core.UUID).get().plugin();
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
			log.debug("Creating default users");
			UserStore.create(UserConfig.newBuilder().setUsername("admin").setPassword("password").build());
			skipped = false;
		}

		// Setup default listeners
		if (ListenerStore.getMetadata().getInitCount() == 1) {
			log.debug("Creating default listeners");
			ListenerStore.create(ListenerConfig.newBuilder().setPort(8768).setAddress("0.0.0.0").setOwner("admin")
					.setName("Default Listener").setEnabled(true).build());
			skipped = false;
		}

		// Setup default groups
		if (GroupStore.getMetadata().getInitCount() == 1) {
			log.debug("Creating default groups");
			GroupStore
					.create(GroupConfig.newBuilder().setName("Default Authentication Group").setOwner("admin").build());
			skipped = false;
		}

		if (skipped)
			return outcome.skipped();

		return outcome.success();
	});

	private Server() {
	}

	static {
		MainDispatch.register(Server.class);
	}

}
