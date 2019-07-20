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
package com.sandpolis.server.vanilla.exe;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.PermissionConstant.server;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCGroup.RQ_AddGroup;
import com.sandpolis.core.proto.net.MCGroup.RQ_GroupDelta;
import com.sandpolis.core.proto.net.MCGroup.RQ_ListGroups;
import com.sandpolis.core.proto.net.MCGroup.RQ_RemoveGroup;
import com.sandpolis.core.proto.net.MCGroup.RS_ListGroups;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.server.vanilla.store.group.Group;
import com.sandpolis.server.vanilla.store.group.GroupStore;

/**
 * Group message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class GroupExe extends Exelet {

	@Auth
	@Permission(permission = server.group.create)
	@Handler(tag = MSG.Message.RQ_ADD_GROUP_FIELD_NUMBER)
	public Message.Builder rq_add_group(RQ_AddGroup rq) {
		var outcome = begin();

		GroupStore.add(rq.getConfig());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_REMOVE_GROUP_FIELD_NUMBER)
	public Message.Builder rq_remove_group(RQ_RemoveGroup rq) {
		var outcome = begin();
		if (!ownership(rq.getId()))
			return failure(outcome, ErrorCode.ACCESS_DENIED);

		GroupStore.remove(rq.getId());
		return success(outcome);
	}

	@Auth
	@Permission(permission = server.group.view)
	@Handler(tag = MSG.Message.RQ_LIST_GROUPS_FIELD_NUMBER)
	public Message.Builder rq_list_groups(RQ_ListGroups rq) {
		var rs = RS_ListGroups.newBuilder();

		// TODO get correct user
		GroupStore.getMembership(null).stream().map(group -> group.extract()).forEach(rs::addGroup);
		return rs;
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_GROUP_DELTA_FIELD_NUMBER)
	public Message.Builder rq_group_delta(RQ_GroupDelta rq) {
		var outcome = begin();
		if (!ownership(rq.getDelta().getConfig().getId()))
			return failure(outcome, ErrorCode.ACCESS_DENIED);

		return complete(outcome, GroupStore.delta(rq.getId(), rq.getDelta()));
	}

	@AccessPredicate
	private boolean ownership(String id) {
		Group group = GroupStore.get(id).orElse(null);
		if (group == null)
			return false;

		return group.getOwner().getCvid() == connector.getRemoteCvid();
	}

	@AccessPredicate
	private boolean membership(String id) {
		Group group = GroupStore.get(id).orElse(null);
		if (group == null)
			return false;

		// TODO
		return false;
	}

}
