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
import java.util.UUID;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GptHeader(

		/**
		 * "EFI PART".
		 */
		String signature,

		/**
		 *
		 */
		int revision,

		/**
		 *
		 */
		int header_size,

		/**
		 *
		 */
		int header_crc,

		/**
		 *
		 */
		int reserved,

		/**
		 *
		 */
		long primary_lba,

		/**
		 *
		 */
		long backup_lba,

		/**
		 *
		 */
		long first_usable_lba,

		/**
		 *
		 */
		long last_usable_lba,

		/**
		 *
		 */
		String guid,

		/**
		 *
		 */
		long first_entry_lba,

		/**
		 *
		 */
		int number_of_entries,

		/**
		 *
		 */
		int size_of_entry,

		/**
		 *
		 */
		int entries_crc) {

	private static final Logger log = LoggerFactory.getLogger(GptHeader.class);

	public static GptHeader read(FileChannel channel) throws IOException {

		var signature = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(signature) != Long.BYTES)
			throw new IOException("Failed to read: signature");

		var revision = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(revision) != Integer.BYTES)
			throw new IOException("Failed to read: revision");

		var header_size = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(header_size) != Integer.BYTES)
			throw new IOException("Failed to read: header_size");

		var header_crc = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(header_crc) != Integer.BYTES)
			throw new IOException("Failed to read: header_crc");

		var reserved = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(reserved) != Integer.BYTES)
			throw new IOException("Failed to read: reserved");

		var primary_lba = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(primary_lba) != Long.BYTES)
			throw new IOException("Failed to read: primary_lba");

		var backup_lba = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(backup_lba) != Long.BYTES)
			throw new IOException("Failed to read: backup_lba");

		var first_usable_lba = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(first_usable_lba) != Long.BYTES)
			throw new IOException("Failed to read: first_usable_lba");

		var last_usable_lba = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(last_usable_lba) != Long.BYTES)
			throw new IOException("Failed to read: last_usable_lba");

		var guid = ByteBuffer.allocate(16);
		if (channel.read(guid) != 16)
			throw new IOException("Failed to read: guid");

		var first_entry_lba = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(first_entry_lba) != Long.BYTES)
			throw new IOException("Failed to read: first_entry_lba");

		var number_of_entries = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(number_of_entries) != Integer.BYTES)
			throw new IOException("Failed to read: number_of_entries");

		var size_of_entry = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(size_of_entry) != Integer.BYTES)
			throw new IOException("Failed to read: size_of_entry");

		var entries_crc = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(entries_crc) != Integer.BYTES)
			throw new IOException("Failed to read: entries_crc");

		var header = new GptHeader( //
				new String(signature.array()), //
				revision.getInt(0), //
				header_size.getInt(0), //
				header_crc.getInt(0), //
				reserved.getInt(0), //
				primary_lba.getLong(0), //
				backup_lba.getLong(0), //
				first_usable_lba.getLong(0), //
				last_usable_lba.getLong(0), //
				new UUID(guid.getLong(0), guid.getLong(1)).toString(), //
				first_entry_lba.getLong(0), //
				number_of_entries.getInt(0), //
				size_of_entry.getInt(0), //
				entries_crc.getInt(0));

		// Validate signature
		if (!header.signature().equals("EFI PART")) {
			throw new RuntimeException();
		}

		// Check CRC32
		var crc = new CRC32();
		crc.update(signature);
		crc.update(revision);
		crc.update(header_size);
		crc.update(0);
		crc.update(0);
		crc.update(0);
		crc.update(0);
		crc.update(reserved);
		crc.update(primary_lba);
		crc.update(backup_lba);
		crc.update(first_usable_lba);
		crc.update(last_usable_lba);
		crc.update(guid);
		crc.update(first_entry_lba);
		crc.update(number_of_entries);
		crc.update(size_of_entry);
		crc.update(entries_crc);

		if (crc.getValue() != header.header_crc()) {
			log.info("Detected corrupt GPT header");
		}

		log.trace("Parsed GPT header: {}", header);
		return header;
	}
}
