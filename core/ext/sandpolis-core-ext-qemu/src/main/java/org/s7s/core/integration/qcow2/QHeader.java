//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.qcow2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Represents a qcow2 header structure.
 */
public record QHeader(

		/**
		 * The format's magic number.
		 */
		int magic,

		/**
		 * The format's version number.
		 */
		int version,

		/**
		 * Offset into the image file at which the backing file name is stored.
		 */
		long backing_file_offset,

		/**
		 * Length of the backing file name in bytes. Must not be longer than 1023 bytes.
		 * Undefined if the image doesn't have a backing file.
		 */
		int backing_file_size,

		/**
		 * Number of bits that are used for addressing an offset within a cluster (1 <<
		 * cluster_bits is the cluster size). Must not be less than 9 (i.e. 512 byte
		 * clusters).
		 */
		int cluster_bits,

		/**
		 * Virtual disk size in bytes.
		 */
		long size,

		/**
		 * Cluster encryption method.
		 */
		int crypt_method,

		/**
		 * Number of entries in the active L1 table.
		 */
		int l1_size,

		/**
		 * Offset into the image file at which the active L1 table starts. Must be
		 * aligned to a cluster boundary.
		 */
		long l1_table_offset,

		/**
		 * Offset into the image file at which the refcount table starts. Must be
		 * aligned to a cluster boundary.
		 */
		long refcount_table_offset,

		/**
		 * Number of clusters that the refcount table occupies.
		 */
		int refcount_table_clusters,

		/**
		 * Number of snapshots contained in the image.
		 */
		int nb_snapshots,

		/**
		 * Offset into the image file at which the snapshot table starts. Must be
		 * aligned to a cluster boundary.
		 */
		long snapshots_offset) {

	public static QHeader read(FileChannel channel) throws IOException, IllegalHeaderException {

		var magic = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(magic) != Integer.BYTES)
			throw new IOException("Failed to read: magic");

		var version = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(version) != Integer.BYTES)
			throw new IOException("Failed to read: version");

		var backing_file_offset = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(backing_file_offset) != Long.BYTES)
			throw new IOException("Failed to read: backing_file_offset");

		var backing_file_size = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(backing_file_size) != Integer.BYTES)
			throw new IOException("Failed to read: backing_file_size");

		var cluster_bits = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(cluster_bits) != Integer.BYTES)
			throw new IOException("Failed to read: cluster_bits");

		var size = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(size) != Long.BYTES)
			throw new IOException("Failed to read: size");

		var crypt_method = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(crypt_method) != Integer.BYTES)
			throw new IOException("Failed to read: crypt_method");

		var l1_size = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(l1_size) != Integer.BYTES)
			throw new IOException("Failed to read: l1_size");

		var l1_table_offset = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(l1_table_offset) != Long.BYTES)
			throw new IOException("Failed to read: l1_table_offset");

		var refcount_table_offset = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(refcount_table_offset) != Long.BYTES)
			throw new IOException("Failed to read: refcount_table_offset");

		var refcount_table_clusters = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(refcount_table_clusters) != Integer.BYTES)
			throw new IOException("Failed to read: refcount_table_clusters");

		var nb_snapshots = ByteBuffer.allocate(Integer.BYTES);
		if (channel.read(nb_snapshots) != Integer.BYTES)
			throw new IOException("Failed to read: nb_snapshots");

		var snapshots_offset = ByteBuffer.allocate(Long.BYTES);
		if (channel.read(snapshots_offset) != Long.BYTES)
			throw new IOException("Failed to read: snapshots_offset");

		// Validate magic
		if (magic.getInt(0) != 0x514649fb) {
			throw new IllegalHeaderException("magic", magic.getInt(0));
		}

		// Validate version
		if (version.getInt(0) != 2 && version.getInt(0) != 3) {
			throw new IllegalHeaderException("version", version.getInt(0));
		}

		return new QHeader( //
				magic.getInt(0), //
				version.getInt(0), //
				backing_file_offset.getLong(0), //
				backing_file_size.getInt(0), //
				cluster_bits.getInt(0), //
				size.getLong(0), //
				crypt_method.getInt(0), //
				l1_size.getInt(0), //
				l1_table_offset.getLong(0), //
				refcount_table_offset.getLong(0), //
				refcount_table_clusters.getInt(0), //
				nb_snapshots.getInt(0), //
				snapshots_offset.getLong(0) //
		);
	}

	public void write(FileChannel channel) throws IOException {

		try (var lock = channel.lock(0, header_length(), false)) {
			channel.write(ByteBuffer.allocate(header_length()) //
					.putInt(magic()) //
					.putInt(version()) //
					.putLong(backing_file_offset()) //
					.putLong(backing_file_size()) //
					.putInt(cluster_bits()) //
					.putLong(size()) //
					.putInt(crypt_method()) //
					.putInt(l1_size()) //
					.putLong(l1_table_offset()) //
					.putLong(refcount_table_offset()) //
					.putInt(refcount_table_clusters()) //
					.putInt(nb_snapshots()) //
					.putLong(snapshots_offset()) //
					, 0);
		}
	}

	public static class IllegalHeaderException extends Exception {
		public IllegalHeaderException(String field, Number value) {
			super("Field '" + field + "' was invalid: " + value);
		}
	}

	/**
	 * @return The size of a cluster in bytes.
	 */
	public int cluster_size() {
		return 1 << cluster_bits();
	}

	/**
	 * @return The number of entries in an L2 table.
	 */
	public int l2_entries() {
		return cluster_size() / Long.BYTES;
	}

	/**
	 * @return The total size of the header in bytes.
	 */
	public int header_length() {
		return 72;
	}

	/**
	 * @return The width of a refcount block entry.
	 */
	public int refcount_bits() {
		return 16;
	}
}
