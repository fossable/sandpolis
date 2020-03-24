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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.Result.ErrorCode.FAILURE_KEY_CHALLENGE;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_KEY;
import static com.sandpolis.core.instance.Result.ErrorCode.UNKNOWN_GROUP;
import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.net.util.ProtoUtil.rq;
import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;
import static com.sandpolis.server.vanilla.store.group.GroupStore.GroupStore;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.MsgAuth.RQ_KeyAuth;
import com.sandpolis.core.net.MsgAuth.RQ_NoAuth;
import com.sandpolis.core.net.MsgAuth.RQ_PasswordAuth;
import com.sandpolis.core.net.MsgClient.RQ_ClientMetadata;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.net.handler.sand5.Sand5Handler;
import com.sandpolis.core.profile.AK_CLIENT;
import com.sandpolis.core.profile.AK_INSTANCE;
import com.sandpolis.core.profile.store.Events.ProfileOnlineEvent;
import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.server.vanilla.auth.KeyMechanism;
import com.sandpolis.server.vanilla.store.group.Group;

/**
 * Authentication message handlers.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class AuthExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(AuthExe.class);

	@Unauth
	@Handler(tag = MSG.RQ_NO_AUTH_FIELD_NUMBER)
	public static MessageOrBuilder rq_no_auth(ExeletContext context, RQ_NoAuth rq) {
		var outcome = begin();

		List<Group> groups = GroupStore.getUnauthGroups();
		if (groups.size() == 0) {
			log.debug("Refusing free authentication attempt because there are no unauth groups");
			context.defer(() -> {
				context.connector.close();
			});
			return failure(outcome, UNKNOWN_GROUP);
		}

		context.defer(() -> {
			auth_success(context, groups);
		});

		return success(outcome);
	}

	@Unauth
	@Handler(tag = MSG.RQ_PASSWORD_AUTH_FIELD_NUMBER)
	public static MessageOrBuilder rq_password_auth(ExeletContext context, RQ_PasswordAuth rq) {
		var outcome = begin();

		List<Group> groups = GroupStore.getByPassword(rq.getPassword());
		if (groups.size() == 0) {
			log.debug("Refusing password authentication attempt because the password did not match any group");
			context.defer(() -> {
				context.connector.close();
			});
			return failure(outcome, UNKNOWN_GROUP);
		}

		context.defer(() -> {
			auth_success(context, groups);
		});

		return success(outcome);
	}

	@Unauth
	@Handler(tag = MSG.RQ_KEY_AUTH_FIELD_NUMBER)
	public static MessageOrBuilder rq_key_auth(ExeletContext context, RQ_KeyAuth rq)
			throws InterruptedException, ExecutionException {
		var outcome = begin();

		Group group = GroupStore.get(rq.getGroupId()).orElse(null);
		if (group == null) {
			log.debug("Refusing key authentication attempt due to unknown group ID: {}", rq.getGroupId());
			context.defer(() -> {
				context.connector.close();
			});
			return failure(outcome, UNKNOWN_GROUP);
		}

		KeyMechanism mech = group.getKeyMechanism(rq.getMechId());
		if (mech == null) {
			log.debug("Refusing key authentication attempt due to unknown mechanism ID: {}", rq.getMechId());
			context.defer(() -> {
				context.connector.close();
			});
			return failure(outcome, INVALID_KEY);
		}

		Sand5Handler sand5 = Sand5Handler.newRequestHandler(mech.getServer());
		context.connector.engage(HandlerKey.SAND5, sand5);

		if (sand5.challengeFuture().get()) {
			context.defer(() -> {
				auth_success(context, List.of(group));
			});

			return success(outcome);
		} else {
			log.debug("Refusing key authentication attempt due to failed challenge");
			context.defer(() -> {
				context.connector.close();
			});
			return failure(outcome, FAILURE_KEY_CHALLENGE);
		}
	}

	private static void auth_success(ExeletContext context, List<Group> groups) {

		// Connection is now authenticated
		context.connector.authenticate();

		ProfileStore.get(context.connector.getRemoteUuid()).ifPresentOrElse(profile -> {
			groups.forEach(group -> {
				// TODO add client to group
			});

			ProfileStore.post(ProfileOnlineEvent::new, profile);
		}, () -> {
			// Metadata query
			var future = context.connector.request(rq().setRqClientMetadata(RQ_ClientMetadata.newBuilder()));
			future.addListener(f -> {
				if (future.isSuccess()) {
					var rs = future.get().getRsClientMetadata();
					var profile = new Profile(context.connector.getRemoteUuid(), context.connector.getRemoteInstance(),
							context.connector.getRemoteInstanceFlavor());
					profile.setCvid(context.connector.getRemoteCvid());

					// Set attributes
					profile.set(AK_CLIENT.HOSTNAME, rs.getHostname());
					profile.set(AK_CLIENT.INSTALL_DIRECTORY, rs.getInstallDirectory());
					profile.set(AK_INSTANCE.OS, rs.getOs());

					groups.forEach(group -> {
						// TODO add client to group
					});

					ProfileStore.add(profile);
					ProfileStore.post(ProfileOnlineEvent::new, profile);
				}
			});
		});
	}

	private AuthExe() {
	}
}
