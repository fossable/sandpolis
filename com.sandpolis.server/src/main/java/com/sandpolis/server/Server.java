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

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.complete;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.attribute.AttributeGroup;
import com.sandpolis.core.attribute.AttributeNode;
import com.sandpolis.core.attribute.UntrackedAttribute;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.storage.database.DatabaseFactory;
import com.sandpolis.core.instance.store.database.DatabaseStore;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.ipc.store.IPCStore;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.profile.ProfileStore;
import com.sandpolis.core.proto.ipc.MCMetadata.RS_Metadata;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Generator.NetworkConfig;
import com.sandpolis.core.proto.util.Generator.NetworkTarget;
import com.sandpolis.core.proto.util.Generator.OutputFormat;
import com.sandpolis.core.proto.util.Generator.OutputPayload;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Result.Outcome;
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
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

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

		MainDispatch.register(Server::checkEnvironment, Server.class, "checkEnvironment");
		MainDispatch.register(Server::checkLocks, Server.class, "checkLocks");
		MainDispatch.register(Server::loadServerStores, Server.class, "loadServerStores");
		MainDispatch.register(Server::installDebugClient, Server.class, "installDebugClient");
		MainDispatch.register(Server::loadListeners, Server.class, "loadListeners");
		MainDispatch.register(Server::post, Server.class, "post");
	}

	/**
	 * Check the runtime environment for fatal errors.
	 * 
	 * @return The {@link Outcome} of the task
	 */
	@InitializationTask(fatal = true)
	private static Outcome checkEnvironment() {
		Outcome.Builder outcome = begin("Check runtime environment");

		try {
			Environment.check();
		} catch (RuntimeException e) {
			return failure(outcome, e);
		}

		return success(outcome);
	}

	/**
	 * Check for instance locks. If found, this instance will exit. Otherwise a new
	 * lock is established.
	 * 
	 * @return The {@link Outcome} of the task
	 */
	@InitializationTask(fatal = true)
	private static Outcome checkLocks() {
		Outcome.Builder outcome = begin("Check instance locks");
		if (Config.NO_MUTEX)
			return success(outcome, "Skipped");

		RS_Metadata metadata = IPCStore.queryInstance(Instance.SERVER);
		if (metadata != null) {
			return failure(outcome, "Another server instance has been detected (process " + metadata.getPid() + ")");
		}

		try {
			IPCStore.listen(Instance.SERVER);
		} catch (IOException e) {
			log.warn("Failed to initialize an IPC listener", e);
		}

		return success(outcome);
	}

	/**
	 * Load static stores.
	 * 
	 * @return The {@link Outcome} of the task
	 */
	@InitializationTask
	private static Outcome loadServerStores() {
		Outcome.Builder outcome = begin("Load server stores");

		// Load NetworkStore and choose a new CVID
		Core.setCvid(IDUtil.CVID.cvid(Instance.SERVER));
		NetworkStore.load(Core.cvid());

		// Load PrefStore
		PrefStore.load(Preferences.userRoot());

		// Load DatabaseStore
		try {
			if (Config.DB_URL == null) {
				DatabaseStore.load(DatabaseFactory.create("h2", Environment.DB.resolve("server.db").toFile()),
						ORM_CLASSES);
			} else {
				DatabaseStore.load(DatabaseFactory.create(Config.DB_URL), ORM_CLASSES);
			}
		} catch (URISyntaxException | IOException e) {
			return failure(outcome, e);
		}

		// Load UserStore
		UserStore.load(DatabaseStore.main());

		// Load ListenerStore
		ListenerStore.load(DatabaseStore.main());

		// Load GroupStore
		GroupStore.load(DatabaseStore.main());

		// Load ProfileStore
		ProfileStore.load(DatabaseStore.main());

		return success(outcome);
	}

	/**
	 * Load socket listeners.
	 * 
	 * @return The {@link Outcome} of the task
	 */
	@InitializationTask
	private static Outcome loadListeners() {
		Outcome.Builder outcome = begin("Load socket listeners");

		outcome.setResult(ListenerStore.start().getResult());
		return complete(outcome);
	}

	/**
	 * Install a debug client on the local machine.
	 * 
	 * @return The {@link Outcome} of the task
	 */
	@InitializationTask
	private static Outcome installDebugClient() {
		Outcome.Builder outcome = begin("Install debug client");
		if (!Config.DEBUG_CLIENT)
			return success(outcome, "Skipped");

		try {
			new MegaGen(GenConfig.newBuilder().setPayload(OutputPayload.MEGA).setFormat(OutputFormat.JAR)
					.setMega(MegaConfig.newBuilder()
							.setNetwork(NetworkConfig.newBuilder()
									.addTarget(NetworkTarget.newBuilder().setAddress("127.0.0.1").setPort(10101))))
					.build()).generate();
		} catch (Exception e) {
			return failure(outcome, e);
		}

		return success(outcome);
	}

	/**
	 * Perform a self-test.
	 * 
	 * @return The {@link Outcome} of the task
	 */
	@InitializationTask
	private static Outcome post() {
		Outcome.Builder outcome = begin("Power-on self test");
		if (!Config.POST)
			return success(outcome, "Skipped");

		log.info("Performing POST");
		Outcome post = Post.smokeTest();
		if (post.getResult())
			log.info("POST completed successfully in {} ms", post.getTime());
		return outcome.mergeFrom(post).build();
	}

	/**
	 * A list of classes that are managed by the ORM for this instance.
	 */
	private static final Class<?>[] ORM_CLASSES = new Class<?>[] { Database.class, Listener.class, Group.class,
			ReciprocalKeyPair.class, KeyMechanism.class, PasswordMechanism.class, User.class, Profile.class,
			AttributeNode.class, AttributeGroup.class, UntrackedAttribute.class };

}
