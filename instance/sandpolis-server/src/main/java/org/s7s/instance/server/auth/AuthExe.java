//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.auth;

import static org.s7s.core.foundation.Instance.InstanceType.AGENT;
import static org.s7s.core.instance.profile.ProfileStore.ProfileStore;
import static org.s7s.core.server.group.GroupStore.GroupStore;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.profile.ProfileStore.ProfileOnlineEvent;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.InstanceOids.ProfileOid;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.AgentOid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.server.group.Group;
import org.s7s.core.serveragent.Messages.RQ_AgentMetadata;
import org.s7s.core.serveragent.Messages.RS_AgentMetadata;
import org.s7s.core.protocol.Session.RQ_AuthSession;
import org.s7s.core.protocol.Session.RS_AuthSession;

/**
 * Authentication message handlers.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class AuthExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(AuthExe.class);

	@Handler(auth = false, instances = AGENT)
	public static RS_AuthSession rq_auth_session(ExeletContext context, RQ_AuthSession rq) {

		switch (rq.getAuthMethodCase()) {
		case AUTHMETHOD_NOT_SET:
			var none_groups = GroupStore.getUnauthGroups().collect(Collectors.toList());
			if (none_groups.size() == 0) {
				log.debug("Refusing free authentication attempt because there are no unauth groups");
				context.defer(() -> {
					context.connector.close();
				});
				return RS_AuthSession.AUTH_SESSION_FAILED;
			}

			context.defer(() -> {
				auth_success(context, none_groups);
			});

			return RS_AuthSession.AUTH_SESSION_OK;
		case PASSWORD:
			var password_groups = GroupStore.getByPassword(rq.getPassword()).collect(Collectors.toList());
			if (password_groups.size() == 0) {
				log.debug("Refusing password authentication attempt because the password did not match any group");
				context.defer(() -> {
					context.connector.close();
				});
				return RS_AuthSession.AUTH_SESSION_FAILED;
			}

			context.defer(() -> {
				auth_success(context, password_groups);
			});

			return RS_AuthSession.AUTH_SESSION_OK;
		case TOKEN:
			break;
		}

		return RS_AuthSession.AUTH_SESSION_FAILED;
	}

	private static void auth_success(ExeletContext context, List<Group> groups) {

		// Connection is now authenticated
		context.connector.authenticate();

		ProfileStore.getByUuid(context.connector.get(ConnectionOid.REMOTE_UUID).asString()).ifPresentOrElse(profile -> {
			groups.forEach(group -> {
				// TODO add client to group
			});

			ProfileStore.post(new ProfileOnlineEvent(profile));
		}, () -> {
			// Metadata query
			context.connector.request(RS_AgentMetadata.class, RQ_AgentMetadata.newBuilder()).thenAccept(rs -> {
				var clientProfile = ProfileStore.create(profile -> {
					profile.set(ProfileOid.UUID, context.connector.get(ConnectionOid.REMOTE_UUID).asString());
					profile.set(ProfileOid.INSTANCE_TYPE,
							context.connector.get(ConnectionOid.REMOTE_INSTANCE).asInstanceType());
					profile.set(ProfileOid.INSTANCE_FLAVOR,
							context.connector.get(ConnectionOid.REMOTE_INSTANCE_FLAVOR).asInstanceFlavor());
					profile.set(AgentOid.HOSTNAME, rs.getHostname());
					profile.set(AgentOid.OS_TYPE, rs.getOs());
				});

				groups.forEach(group -> {
					// TODO add client to group
				});

				ProfileStore.post(new ProfileOnlineEvent(clientProfile));
			});
		});
	}

	private AuthExe() {
	}
}
