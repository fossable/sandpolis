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

import static com.sandpolis.core.sv.msg.MsgGroup.RQ_GroupOperation.GroupOperation.GROUP_CREATE;
import static com.sandpolis.core.sv.msg.MsgGroup.RQ_GroupOperation.GroupOperation.GROUP_DELETE;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.concurrent.CompletionStage;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;

import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.sv.msg.MsgGroup.RQ_GroupOperation;

/**
 * An API for interacting with authentication groups on the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GroupCmd extends Cmdlet<GroupCmd> {

	/**
	 * Create a new group.
	 * 
	 * @param name The group name
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> create(GroupConfig config) {
		return request(Outcome.class, RQ_GroupOperation.newBuilder().setOperation(GROUP_CREATE).addGroupConfig(config));
	}

	public CompletionStage<Outcome> remove(String id) {
		return request(Outcome.class, RQ_GroupOperation.newBuilder().setOperation(GROUP_DELETE)
				.addGroupConfig(GroupConfig.newBuilder().setId(id)));
	}

	public CompletionStage<Outcome> exportToFile(File group, String groupId, String password) throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(new SecureRandom(password.getBytes()));

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, keyGen.generateKey());

		// TODO
		return null;
	}

	public CompletionStage<Outcome> importFromFile(File group, String password) throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(new SecureRandom(password.getBytes()));

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, keyGen.generateKey());

		try (InputStream in = new CipherInputStream(new FileInputStream(group), cipher)) {
			GroupConfig container = GroupConfig.parseDelimitedFrom(in);
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
