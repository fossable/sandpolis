//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.init;

import static org.s7s.core.instance.plugin.PluginStore.PluginStore;
import static org.s7s.core.instance.pref.PrefStore.PrefStore;
import static org.s7s.core.instance.profile.ProfileStore.ProfileStore;
import static org.s7s.core.instance.state.STStore.STStore;
import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;
import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;
import static org.s7s.core.instance.exelet.ExeletStore.ExeletStore;
import static org.s7s.core.instance.network.NetworkStore.NetworkStore;
import static org.s7s.core.instance.stream.StreamStore.StreamStore;
import static org.s7s.core.server.banner.BannerStore.BannerStore;
import static org.s7s.core.server.group.GroupStore.GroupStore;
import static org.s7s.core.server.listener.ListenerStore.ListenerStore;
import static org.s7s.core.server.location.LocationStore.LocationStore;
import static org.s7s.core.server.trust.TrustStore.TrustStore;
import static org.s7s.core.server.user.UserStore.UserStore;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import org.s7s.core.instance.Entrypoint;
import org.s7s.core.instance.InitTask;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.EphemeralDocument;
import org.s7s.core.instance.util.S7SSessionID;
import org.s7s.core.server.ServerContext;
import org.s7s.core.server.auth.AuthExe;
import org.s7s.core.server.auth.LoginExe;
import org.s7s.core.server.banner.BannerExe;
import org.s7s.core.server.group.GroupExe;
import org.s7s.core.server.listener.ListenerExe;
import org.s7s.core.server.plugin.PluginExe;
import org.s7s.core.server.state.STExe;
import org.s7s.core.server.stream.StreamExe;
import org.s7s.core.server.user.UserExe;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

public class ServerLoadStores extends InitTask {

	@Override
	public TaskOutcome run(TaskOutcome.Factory outcome) {
		switch (ServerContext.STORAGE_PROVIDER.get()) {
		case "mongodb":
			// TODO
			ServerContext.MONGODB_HOST.get();
			ServerContext.MONGODB_USER.get();
			ServerContext.MONGODB_PASSWORD.get();
			break;
		case "embedded":
			// TODO
			break;
		case "ephemeral":
			STStore.init(config -> {
				config.concurrency = 2;
				config.root = new EphemeralDocument(null, null);
			});
			break;
		default:
			return outcome.failed();
		}

		ProfileStore.init(config -> {
			config.collection = STStore.get(Oid.of("/profile"));
		});

		ThreadStore.init(config -> {
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2));
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("server.generator", Executors.newCachedThreadPool());
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		NetworkStore.init(config -> {
			config.sid = S7SSessionID.of(Entrypoint.data().instance(), Entrypoint.data().flavor()).sid();
			config.collection = STStore.get(Oid.of("/network_connection"));
		});

		ConnectionStore.init(config -> {
			config.collection = STStore.get(Oid.of("/connection"));
		});

		ExeletStore.init(config -> {
			config.exelets.addAll(List.of(AuthExe.class, GroupExe.class, ListenerExe.class, LoginExe.class,
					BannerExe.class, UserExe.class, PluginExe.class, StreamExe.class, STExe.class));
		});

		StreamStore.init(config -> {
		});

		PrefStore.init(config -> {
			config.instance = InstanceType.SERVER;
			config.flavor = Entrypoint.data().flavor();
		});

		BannerStore.init(config -> {
		});

		UserStore.init(config -> {
			config.collection = STStore.get(Oid.of("/user"));
		});

		ListenerStore.init(config -> {
			config.collection = STStore.get(Oid.of("/profile/*/server/listener", Entrypoint.data().uuid()));
		});

		GroupStore.init(config -> {
			config.collection = STStore.get(Oid.of("/group"));
		});

		TrustStore.init(config -> {
			config.collection = STStore.get(Oid.of("/trust_anchor"));
		});

		PluginStore.init(config -> {
			config.verifier = TrustStore::verifyPluginCertificate;
			config.collection = STStore.get(Oid.of("/profile/*/plugin", Entrypoint.data().uuid()));
		});

		LocationStore.init(config -> {
			config.service = ServerContext.GEOLOCATION_SERVICE.get();
			config.key = ServerContext.GEOLOCATION_SERVICE_KEY.get();
			config.cacheExpiration = Duration.ofDays(10);
		});

		return outcome.succeeded();
	}

	@Override
	public String description() {
		return "Load stores";
	}

	@Override
	public boolean fatal() {
		return true;
	}

}
