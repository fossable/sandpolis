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
package com.sandpolis.server;

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.database.DatabaseStore;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Auth.PasswordContainer;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.server.auth.KeyMechanism;
import com.sandpolis.server.store.group.Group;
import com.sandpolis.server.store.group.GroupStore;
import com.sandpolis.server.store.listener.ListenerStore;
import com.sandpolis.server.store.user.UserStore;

public final class Post {
	private Post() {
	}

	public static Outcome smokeTest() {
		Outcome.Builder outcome = begin();
		Outcome test;

		try {
			// Check UserStore
			test = UserStore.add(UserConfig.newBuilder().setUsername("POSTUSER").setPassword("POSTPASS").build());
			if (!test.getResult())
				return failure(outcome.mergeFrom(test));

			// Check ListenerStore
			test = ListenerStore.add(ListenerConfig.newBuilder().setId(2).setPort(7000).setAddress("0.0.0.0")
					.setOwner("POSTUSER").setName("POST").build());
			if (!test.getResult())
				return failure(outcome.mergeFrom(test));

			// Check GroupStore
			test = GroupStore.add(GroupConfig.newBuilder().setId(2).setName("POSTGROUP").setOwner("POSTUSER")
					.addPasswordMechanism(PasswordContainer.newBuilder().setPassword("POSTPASS")).build());
			if (!test.getResult())
				return failure(outcome.mergeFrom(test));

			Group testGroup = GroupStore.get(2L);
			testGroup.addKeyMechanism(KeyMechanism.generate(testGroup));
			if (testGroup.getKeys().size() != 1)
				return failure(outcome, "Unexpected size: " + testGroup.getKeys().size());

			// Check DatabaseStore
			DatabaseStore.add(new Database("jdbc:mysql://127.0.0.1/test"));
		} catch (Exception e) {
			return failure(outcome, e);
		}

		return success(outcome);
	}
}
