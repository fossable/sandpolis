//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.qcow2;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RefcountTable {

	public record RefcountTableEntry(long data) {

		public long offset() {
			return data() & 0xfffffffffffffe00L;
		}
	}

	public record RefcountBlockEntry(long data) {

		public long refcount() {
			return data() & 0xffffL;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(RefcountTable.class);

	private final FileChannel channel;

	private final Qcow2 qcow2;

	private final RefcountTableEntry[] refcount_table;

	public RefcountTable(Qcow2 qcow2) throws IOException {
		this.qcow2 = qcow2;
		this.channel = FileChannel.open(qcow2.file, READ, WRITE);

		refcount_table = readRefcountTable();
	}

	public void increment_all() {
		// TODO
	}

	public long lookup_refcount(long image_offset) throws IOException {

		long refcount_block_entries = (qcow2.header.cluster_size() * 8) / qcow2.header.refcount_bits();

		int refcount_block_index = (int) ((image_offset / qcow2.header.cluster_size()) % refcount_block_entries);
		int refcount_table_index = (int) ((image_offset / qcow2.header.cluster_size()) / refcount_block_entries);

		var refcount_block = readRefcountBlock(refcount_table[refcount_table_index]);
		return refcount_block[refcount_block_index].refcount();
	}

	private RefcountBlockEntry[] readRefcountBlock(RefcountTableEntry refcount_table_entry) throws IOException {

		var block_buffer = ByteBuffer.allocateDirect(qcow2.header.cluster_size());
		if (channel.read(block_buffer, refcount_table_entry.offset()) != qcow2.header.cluster_size()) {
			throw new IOException();
		}

		var table = new RefcountBlockEntry[(qcow2.header.cluster_size() * 8) / qcow2.header.refcount_bits()];

		block_buffer.position(0);
		for (int i = 0; i < table.length; i++) {
			table[i] = new RefcountBlockEntry(block_buffer.getLong());
		}

		return table;
	}

	private RefcountTableEntry[] readRefcountTable() throws IOException {

		log.debug("Loading refcount table ({} bytes) from offset: 0x{}",
				qcow2.header.refcount_table_clusters() * qcow2.header.cluster_size(),
				Long.toHexString(qcow2.header.refcount_table_offset()));

		var table_buffer = ByteBuffer
				.allocateDirect(qcow2.header.refcount_table_clusters() * qcow2.header.cluster_size());
		if (channel.read(table_buffer, qcow2.header.refcount_table_offset()) != qcow2.header.refcount_table_clusters()
				* qcow2.header.cluster_size()) {
			throw new IOException();
		}

		var table = new RefcountTableEntry[qcow2.header.refcount_table_clusters() * qcow2.header.cluster_size()
				/ Long.BYTES];

		table_buffer.position(0);
		for (int i = 0; i < table.length; i++) {
			table[i] = new RefcountTableEntry(table_buffer.getLong());
		}

		return table;
	}
}
