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
import com.sandpolis.core.proto.net.MsgGroup.RQ_AddGroup;
import com.sandpolis.core.proto.net.MsgGroup.RQ_RemoveGroup;
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
