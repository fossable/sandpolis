/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.viewer.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;

import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCGroup.RQ_AddGroup;
import com.sandpolis.core.proto.net.MCGroup.RQ_RemoveGroup;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Group.ProtoGroup;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * Contains group commands.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GroupCmd extends Cmdlet<GroupCmd> {

	public ResponseFuture<Outcome> create(String name) {
		return request(RQ_AddGroup.newBuilder().setConfig(GroupConfig.newBuilder().setName(name)));
	}

	public ResponseFuture<Outcome> remove(String id) {
		return request(RQ_RemoveGroup.newBuilder().setId(id));
	}

	public static ResponseFuture<Outcome> exportToFile(File group, String groupId, String password) throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(new SecureRandom(password.getBytes()));

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, keyGen.generateKey());

		// TODO
		return null;
	}

	public static ResponseFuture<Outcome> importFromFile(File group, String password) throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(new SecureRandom(password.getBytes()));

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, keyGen.generateKey());

		try (InputStream in = new CipherInputStream(new FileInputStream(group), cipher)) {
			ProtoGroup container = ProtoGroup.parseDelimitedFrom(in);
		}

		// TODO
		return null;
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link GroupCmd} can be invoked
	 */
	public static GroupCmd async() {
		return new GroupCmd();
	}

	private GroupCmd() {
	}
}
