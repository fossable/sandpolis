/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.server.exe;

import java.util.stream.Stream;

import com.sandpolis.core.instance.Perm;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCGroup.RS_ListGroups;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.server.store.group.Group;
import com.sandpolis.server.store.group.GroupStore;

/**
 * Group message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class GroupExe extends Exelet {

	public GroupExe(Sock connector) {
		super(connector);
	}

	@Auth
	@Permission(permission = Perm.server.groups.create)
	public void rq_add_group(Message m) {
		var rq = m.getRqAddGroup();
		reply(m, GroupStore.add(rq.getConfig()));
	}

	@Auth
	public void rq_remove_group(Message m) {
		var rq = m.getRqRemoveGroup();
		if (!accessCheck(m, this::ownership, rq.getId()))
			return;

		reply(m, GroupStore.remove(rq.getId()));
	}

	@Auth
	public void rq_list_groups(Message m) {
		// TODO check for Perm.server.groups.view

		RS_ListGroups.Builder rs = RS_ListGroups.newBuilder();
		try (Stream<Group> groups = GroupStore.getMembership(null)) {// TODO correct user
			groups.map(group -> group.extract()).forEach(group -> rs.addGroup(group));
		}

		reply(m, rs);
	}

	@Auth
	public void rq_group_delta(Message m) {
		var rq = m.getRqGroupDelta();
		if (!accessCheck(m, this::ownership, rq.getDelta().getConfig().getId()))
			return;

		reply(m, GroupStore.delta(rq.getId(), rq.getDelta()));
	}

	@AccessPredicate
	private boolean ownership(long id) {
		Group group = GroupStore.get(id);
		if (group == null)
			return false;

		return group.getOwner().getCvid() == connector.getRemoteCvid();
	}

	@AccessPredicate
	private boolean membership(long id) {
		Group group = GroupStore.get(id);
		if (group == null)
			return false;

		// TODO
		return false;
	}

}
