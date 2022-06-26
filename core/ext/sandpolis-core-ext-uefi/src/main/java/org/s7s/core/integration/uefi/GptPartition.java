//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.uefi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GptPartition(

		/**
		 *
		 */
		String type_guid,

		/**
		 *
		 */
		String unique_guid,

		/**
		 *
		 */
		long first_lba,

		/**
		 *
		 */
		long last_lba,

		/**
		 *
		 */
		long attributes,

		/**
		 *
		 */
		String partition_name) {

	private static final Logger log = LoggerFactory.getLogger(GptPartition.class);

	public static GptPartition read(FileChannel channel) throws IOException {

		var type_guid = ByteBuffer.allocate(16);
		if (channel.read(type_guid) != 16)
			throw new IOException("Failed to read: type_guid");

		var unique_guid = ByteBuffer.allocate(16);
		if (channel.read(unique_guid) != 16)
			throw new IOException("Failed to read: unique_guid");

		var first_lba = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(first_lba) != Long.BYTES)
			throw new IOException("Failed to read: first_lba");

		var last_lba = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(last_lba) != Long.BYTES)
			throw new IOException("Failed to read: last_lba");

		var attributes = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(attributes) != Long.BYTES)
			throw new IOException("Failed to read: attributes");

		var partition_name = ByteBuffer.allocate(72).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(partition_name) != 72)
			throw new IOException("Failed to read: partition_name");

		var partition = new GptPartition( //
				new UUID(type_guid.getLong(0), type_guid.getLong(1)).toString(), //
				new UUID(unique_guid.getLong(0), unique_guid.getLong(1)).toString(), //
				first_lba.getLong(0), //
				last_lba.getLong(0), //
				attributes.getLong(0), //
				new String(partition_name.array(), StandardCharsets.UTF_16LE));

		log.trace("Parsed GPT partition: {}", partition);
		return partition;
	}
}
