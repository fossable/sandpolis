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

class ClusterTable {

	public record L1Entry(long data) {

		public long offset() {
			return data() & 0x00fffffffffffe00L;
		}
	}

	public record L2Entry(long data) {

		public L2StandardDescriptor standard_descriptor() {
			return new L2StandardDescriptor(data() & ((1 << 62) - 1));
		}

		public L2CompressedDescriptor compressed_descriptor() {
			return new L2CompressedDescriptor(data() & ((1 << 62) - 1));
		}

		public boolean is_compressed() {
			return ((data() >> 62) & 1) == 1;
		}

		public boolean is_unused_compressed_or_cow() {
			return ((data() >> 63) & 1) == 0;
		}
	}

	public record L2StandardDescriptor(long data) {

		public long all_zeros() {
			return data() & 1;
		}

		public long offset() {
			return data() & 0x00fffffffffffe00L;
		}
	}

	public record L2CompressedDescriptor(long data) {

		public long offset() {
			return data() & 0x3fffffffffffffffL;
		}
	}

	private final FileChannel channel;

	public final L1Entry[] l1_table;

	private final Qcow2 qcow2;

	public ClusterTable(Qcow2 qcow2) throws IOException {
		this.qcow2 = qcow2;
		this.channel = FileChannel.open(qcow2.file, READ, WRITE);
		this.l1_table = readL1Table();
	}

	public int read(ByteBuffer data, long vOffset) throws IOException {

		System.out.println("Read request for " + data.capacity() + " bytes at virtual offset: " + vOffset);

		if (vOffset >= qcow2.header.size()) {
			return -1;
		}

		int read = 0;
		final long end = vOffset + qcow2.header.cluster_size();

		while (vOffset < end) {
			int l2_index = (int) ((vOffset / qcow2.header.cluster_size()) % qcow2.header.l2_entries());
			int l1_index = (int) ((vOffset / qcow2.header.cluster_size()) / qcow2.header.l2_entries());
			vOffset += qcow2.header.cluster_size();

			L1Entry l1_table_entry = l1_table[l1_index];
			System.out.println("l1_table[" + l1_index + "].offset = " + l1_table_entry.offset());

			if (l1_table_entry.offset() == 0) {
				int increment = Math.min(data.capacity() - data.position(), qcow2.header.cluster_size());
				read += increment;
				data.position(data.position() + increment);
				continue;
			}

			L2Entry[] l2_table = readL2Table(l1_table[l1_index]);
			L2Entry l2_table_entry = l2_table[l2_index];

			if (l2_table_entry.is_compressed()) {
				// TODO
			} else {

				System.out.println(
						"l2_table[" + l2_index + "].offset = " + l2_table_entry.standard_descriptor().offset());
				if (l2_table_entry.standard_descriptor().offset() == 0) {
					int increment = Math.min(data.capacity() - data.position(), qcow2.header.cluster_size());
					read += increment;
					data.position(data.position() + increment);
					continue;
				}

				System.out.println("Reading from real offset: " + l2_table_entry.standard_descriptor().offset());
				read += channel.read(data,
						l2_table_entry.standard_descriptor().offset() + (vOffset % qcow2.header.cluster_size()));
			}
		}

		data.position(0);
		return read;
	}

	private L1Entry[] readL1Table() throws IOException {

		System.out.println("Loading L1 table at: " + qcow2.header.l1_table_offset());

		var table_buffer = ByteBuffer.allocateDirect(qcow2.header.l1_size() * Long.BYTES);
		if (channel.read(table_buffer, qcow2.header.l1_table_offset()) != qcow2.header.l1_size() * Long.BYTES) {
			throw new IOException();
		}

		var table = new L1Entry[qcow2.header.l1_size()];

		table_buffer.position(0);
		for (int i = 0; i < table.length; i++) {
			table[i] = new L1Entry(table_buffer.getLong());
			System.out.println(
					"Loaded L1 entry: " + Long.toBinaryString(table[i].data()) + " [offset=" + table[i].offset() + "]");
		}

		return table;
	}

	private L2Entry[] readL2Table(L1Entry l1_entry) throws IOException {

		System.out.println("Loading L2 table at: " + l1_entry.offset());

		var table_buffer = ByteBuffer.allocateDirect(qcow2.header.l2_entries() * Long.BYTES);
		if (channel.read(table_buffer, l1_entry.offset()) != qcow2.header.l2_entries() * Long.BYTES) {
			throw new IOException();
		}

		var table = new L2Entry[qcow2.header.l2_entries()];

		table_buffer.position(0);
		for (int i = 0; i < table.length; i++) {
			table[i] = new L2Entry(table_buffer.getLong());
		}

		return table;
	}

	public int write(ByteBuffer data, long vOffset) {

		int l2_index = (int) ((vOffset / qcow2.header.cluster_size()) % qcow2.header.l2_entries());
		int l1_index = (int) ((vOffset / qcow2.header.cluster_size()) / qcow2.header.l2_entries());

		return 0;
	}
}
