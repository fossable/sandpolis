//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.auth;

import static com.sandpolis.core.foundation.Result.ErrorCode.UNKNOWN_GROUP;
import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.Metatypes.InstanceType.AGENT;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.server.group.GroupStore.GroupStore;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.serveragent.msg.MsgAuth.RQ_NoAuth;
import com.sandpolis.core.serveragent.msg.MsgAuth.RQ_PasswordAuth;
import com.sandpolis.core.serveragent.msg.MsgClient.RQ_AgentMetadata;
import com.sandpolis.core.serveragent.msg.MsgClient.RS_AgentMetadata;
import com.sandpolis.core.instance.profile.ProfileEvents.ProfileOnlineEvent;
import com.sandpolis.core.instance.state.AgentOid;
import com.sandpolis.core.instance.state.ConnectionOid;
import com.sandpolis.core.instance.state.ProfileOid;
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

	@Handler(auth = false, instances = AGENT)
	public static MessageLiteOrBuilder rq_no_auth(ExeletContext context, RQ_NoAuth rq) {
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

	@Handler(auth = false, instances = AGENT)
	public static MessageLiteOrBuilder rq_password_auth(ExeletContext context, RQ_PasswordAuth rq) {
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

		ProfileStore.getByUuid(context.connector.get(ConnectionOid.REMOTE_UUID)).ifPresentOrElse(profile -> {
			groups.forEach(group -> {
				// TODO add client to group
			});

			ProfileStore.post(ProfileOnlineEvent::new, profile);
		}, () -> {
			// Metadata query
			context.connector.request(RS_AgentMetadata.class, RQ_AgentMetadata.newBuilder()).thenAccept(rs -> {
				var clientProfile = ProfileStore.create(profile -> {
					profile.set(ProfileOid.UUID, context.connector.get(ConnectionOid.REMOTE_UUID));
					profile.set(ProfileOid.INSTANCE_TYPE, context.connector.get(ConnectionOid.REMOTE_INSTANCE));
					profile.set(ProfileOid.INSTANCE_FLAVOR,
							context.connector.get(ConnectionOid.REMOTE_INSTANCE_FLAVOR));
					profile.set(AgentOid.HOSTNAME, rs.getHostname());
					profile.set(AgentOid.LOCATION, rs.getInstallDirectory());
					profile.set(AgentOid.OS, rs.getOs());
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
