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
import java.util.ArrayList;
import java.util.List;

class SnapshotTable {

	public static record Entry(

			/**
			 * Offset into the image file at which the L1 table for the snapshot starts.
			 * Must be aligned to a cluster boundary.
			 */
			long l1_table_offset,

			/**
			 * Number of entries in the L1 table of the snapshots.
			 */
			int l1_size,

			/**
			 * Length of the unique ID string describing the snapshot
			 */
			short id_str_size,

			/**
			 * Length of the name of the snapshot
			 */
			short name_size,

			/**
			 * Time at which the snapshot was taken in seconds since the Epoch.
			 */
			int date_sec,

			/**
			 * Subsecond part of the time at which the snapshot was taken in nanoseconds.
			 */
			int date_nsec,

			/**
			 * Time that the guest was running until the snapshot was taken in nanoseconds.
			 */
			long vm_clock_nsec,

			/**
			 * The size of associated VM memory in bytes.
			 */
			int vm_state_size,

			/**
			 * The size of the additional data in bytes.
			 */
			int extra_data_size,

			/**
			 * Additional data.
			 */
			byte[] extra_data,

			/**
			 * The snapshot ID.
			 */
			String id_str,

			/**
			 * The snapshot name.
			 */
			String name) {

		public static Entry read(FileChannel channel) throws IOException {

			var l1_table_offset = ByteBuffer.allocate(Long.BYTES);
			if (channel.read(l1_table_offset) != Long.BYTES)
				throw new IOException("Failed to read: l1_table_offset");

			var l1_size = ByteBuffer.allocate(Integer.BYTES);
			if (channel.read(l1_size) != Integer.BYTES)
				throw new IOException("Failed to read: l1_size");

			var id_str_size = ByteBuffer.allocate(Short.BYTES);
			if (channel.read(id_str_size) != Short.BYTES)
				throw new IOException("Failed to read: id_str_size");

			var name_size = ByteBuffer.allocate(Short.BYTES);
			if (channel.read(name_size) != Short.BYTES)
				throw new IOException("Failed to read: name_size");

			var date_sec = ByteBuffer.allocate(Integer.BYTES);
			if (channel.read(date_sec) != Integer.BYTES)
				throw new IOException("Failed to read: date_sec");

			var date_nsec = ByteBuffer.allocate(Integer.BYTES);
			if (channel.read(date_nsec) != Integer.BYTES)
				throw new IOException("Failed to read: date_nsec");

			var vm_clock_nsec = ByteBuffer.allocate(Long.BYTES);
			if (channel.read(vm_clock_nsec) != Long.BYTES)
				throw new IOException("Failed to read: vm_clock_nsec");

			var vm_state_size = ByteBuffer.allocate(Integer.BYTES);
			if (channel.read(vm_state_size) != Integer.BYTES)
				throw new IOException("Failed to read: vm_state_size");

			var extra_data_size = ByteBuffer.allocate(Integer.BYTES);
			if (channel.read(extra_data_size) != Integer.BYTES)
				throw new IOException("Failed to read: extra_data_size");

			var extra_data = ByteBuffer.allocate(extra_data_size.getInt());
			if (channel.read(extra_data) != extra_data_size.getInt())
				throw new IOException("Failed to read: extra_data");

			var id_str = ByteBuffer.allocate(id_str_size.getShort());
			if (channel.read(id_str) != id_str_size.getShort())
				throw new IOException("Failed to read: id_str");

			var name = ByteBuffer.allocate(name_size.getShort());
			if (channel.read(name) != name_size.getShort())
				throw new IOException("Failed to read: name");

			return new Entry( //
					l1_table_offset.getLong(), //
					l1_size.getInt(), //
					id_str_size.getShort(), //
					name_size.getShort(), //
					date_sec.getInt(), //
					date_nsec.getInt(), //
					vm_clock_nsec.getLong(), //
					vm_state_size.getInt(), //
					extra_data_size.getInt(), //
					extra_data.array(), //
					new String(id_str.array()), //
					new String(name.array()) //
			);
		}

		public void write(FileChannel channel) throws IOException {

			var total_length = id_str_size() + name_size() + extra_data_size();

			try (var lock = channel.lock(channel.position(), total_length, false)) {
				channel.write(ByteBuffer.allocate(total_length) //
						.putLong(l1_table_offset()) //
						.putInt(l1_size()) //
						.putShort(id_str_size()) //
						.putInt(name_size()) //
						.putInt(date_sec()) //
						.putInt(date_nsec()) //
						.putLong(vm_clock_nsec()) //
						.putInt(vm_state_size()) //
						.putInt(extra_data_size()) //
						.put(extra_data()) //
						.put(id_str().getBytes()) //
						.put(name().getBytes()));
			}
		}
	}

	private final FileChannel channel;

	private final List<Entry> entries;

	private final Qcow2 qcow2;

	public SnapshotTable(Qcow2 qcow2) throws IOException {
		this.qcow2 = qcow2;

		if (qcow2.header.snapshots_offset() != 0) {
			channel = FileChannel.open(qcow2.file, READ, WRITE);
			channel.position(qcow2.header.snapshots_offset());

			entries = new ArrayList<>();
			for (int i = 0; i < qcow2.header.nb_snapshots(); i++) {
				entries.add(Entry.read(channel));
			}
		} else {
			entries = null;
			channel = null;
		}
	}

	/**
	 * Revert the state of the image to the given snapshot.
	 *
	 * @param id The ID of the snapshot to apply
	 * @throws IOException
	 */
	public void apply(String id) throws IOException {
		try (var lock = channel.lock()) {

			// Find the snapshot
			for (var snapshot : entries) {
				if (snapshot.id_str().equals(id)) {
					// TODO
					new QHeader(qcow2.header.magic(), qcow2.header.version(), qcow2.header.backing_file_offset(),
							qcow2.header.backing_file_size(), qcow2.header.cluster_bits(), qcow2.header.size(),
							qcow2.header.crypt_method(), qcow2.header.l1_size(), 0,
							qcow2.header.refcount_table_offset(), qcow2.header.refcount_table_clusters(),
							qcow2.header.nb_snapshots(), qcow2.header.snapshots_offset());
					break;
				}
			}
		}
	}

	/**
	 * Create a new snapshot from the image's current state.
	 *
	 * @param id   The ID of the new snapshot
	 * @param name The name of the new snapshot
	 * @throws IOException
	 */
	public void take(String id, String name) throws IOException {
		try (var lock = channel.lock()) {

			// Copy current L1 table
			var l1_table = ByteBuffer.allocate(qcow2.header.l1_size() * Long.BYTES);
			channel.read(l1_table, qcow2.header.l1_table_offset());

			// Write table to end
			long l1_table_offset = channel.size();
			channel.write(l1_table, l1_table_offset);

			// Write padding
			channel.write(ByteBuffer.allocate(0 /* TODO */), l1_table_offset + l1_table.capacity());

			// Increment reference counts
			qcow2.refcount_table.increment_all();

			// Capture current timestamp
			var nano_time = System.nanoTime();

			// Create snapshot entry and write it to the table
			var entry = new Entry(l1_table_offset, qcow2.header.l1_size(), (short) id.length(), (short) name.length(),
					(int) (nano_time / 1e9), (int) (nano_time % 1e9), 0, 0, 0, new byte[0], id, name);
			entries.add(entry);
			entry.write(channel);
		}
	}
}
