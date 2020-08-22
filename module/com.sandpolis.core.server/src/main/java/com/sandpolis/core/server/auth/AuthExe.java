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
package com.sandpolis.core.server.auth;

import static com.sandpolis.core.foundation.Result.ErrorCode.UNKNOWN_GROUP;
import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.Metatypes.InstanceType.CLIENT;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.server.group.GroupStore.GroupStore;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.cs.msg.MsgAuth.RQ_NoAuth;
import com.sandpolis.core.cs.msg.MsgAuth.RQ_PasswordAuth;
import com.sandpolis.core.cs.msg.MsgClient.RQ_ClientMetadata;
import com.sandpolis.core.cs.msg.MsgClient.RS_ClientMetadata;
import com.sandpolis.core.instance.profile.ProfileEvents.ProfileOnlineEvent;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletContext;
import com.sandpolis.core.server.group.Group;

/**
 * Authentication message handlers.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class AuthExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(AuthExe.class);

	@Handler(auth = false, instances = CLIENT)
	public static MessageOrBuilder rq_no_auth(ExeletContext context, RQ_NoAuth rq) {
		var outcome = begin();

		var groups = GroupStore.getUnauthGroups().collect(Collectors.toList());
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

	@Handler(auth = false, instances = CLIENT)
	public static MessageOrBuilder rq_password_auth(ExeletContext context, RQ_PasswordAuth rq) {
		var outcome = begin();

		var groups = GroupStore.getByPassword(rq.getPassword()).collect(Collectors.toList());
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

	private static void auth_success(ExeletContext context, List<Group> groups) {

		// Connection is now authenticated
		context.connector.authenticate();

		ProfileStore.getByUuid(context.connector.getRemoteUuid()).ifPresentOrElse(profile -> {
			groups.forEach(group -> {
				// TODO add client to group
			});

			ProfileStore.post(ProfileOnlineEvent::new, profile);
		}, () -> {
			// Metadata query
			context.connector.request(RS_ClientMetadata.class, RQ_ClientMetadata.newBuilder()).thenAccept(rs -> {
				var clientProfile = ProfileStore.create(profile -> {
					profile.uuid().set(context.connector.getRemoteUuid());
					profile.instanceType().set(context.connector.getRemoteInstance());
					profile.instanceFlavor().set(context.connector.getRemoteInstanceFlavor());
					profile.client().hostname().set(rs.getHostname());
					profile.client().location().set(rs.getInstallDirectory());
					profile.client().os().set(rs.getOs());
				});

				groups.forEach(group -> {
					// TODO add client to group
				});

				ProfileStore.post(ProfileOnlineEvent::new, clientProfile);
			});
		});
	}

	private AuthExe() {
	}
}
