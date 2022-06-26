//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.qcow2;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.integration.qcow2.QHeader.IllegalHeaderException;

public class Qcow2 implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(Qcow2.class);

	private final FileChannel channel;

	ClusterTable cluster_table;

	/**
	 * The qcow2 file.
	 */
	public final Path file;

	QHeader header;

	/**
	 * The current read/write position.
	 */
	private long position;

	RefcountTable refcount_table;

	SnapshotTable snapshot_table;

	public Qcow2(Path file) throws IOException, IllegalHeaderException {
		if (!Files.exists(file)) {
			throw new FileNotFoundException();
		}
		log.debug("Opening qcow2 file: {}", file.toAbsolutePath().toString());

		this.file = file;
		this.channel = FileChannel.open(file, READ, WRITE);
		this.header = QHeader.read(channel);
		this.snapshot_table = new SnapshotTable(this);
		this.refcount_table = new RefcountTable(this);
		this.cluster_table = new ClusterTable(this);
	}

	public Qcow2(Path file, long size, long cluster_size) throws IOException {
		if (Files.exists(file)) {
			throw new IllegalArgumentException();
		}
		if (Long.bitCount(cluster_size) != 1) {
			throw new IllegalArgumentException("Cluster size must be a power of 2");
		}

		log.debug("Opening qcow2 file: {}", file.toAbsolutePath().toString());

		this.file = file;
		this.channel = FileChannel.open(file, READ, WRITE, CREATE);

		// Layout L1 table
		long l1_table_offset = cluster_size * 3; // TODO

		// layout refcount table
		long refcount_table_offset = cluster_size * 4; // TODO

		// Write header
		this.header = new QHeader(0x514649fb, 3, 0, 0, 0, size, 0, 0, l1_table_offset, refcount_table_offset, 1, 0, 0);
		this.header.write(channel);
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	/**
	 * Copy the bytes at the given source to the destination. This method may reuse
	 * existing clusters when possible and therefore can be more efficient than a
	 * {@link #read(ByteBuffer)} followed by a {@link #write(ByteBuffer)}.
	 *
	 * @param source      The source offset
	 * @param size        The number of bytes to copy
	 * @param destination The destination offset
	 * @return The number of bytes copied
	 */
	public int copy(long source, long size, long destination) throws IOException {
		// TODO
		return -1;
	}

	/**
	 * @return A new {@code InputStream} containing the virtual data.
	 */
	public QcowInputStream newInputStream() {
		if (!channel.isOpen()) {
			throw new IllegalStateException("The channel is closed");
		}

		log.trace("Client request for new input stream");
		return new QcowInputStream(this);
	}

	/**
	 * @return The read/write pointer.
	 */
	public long position() {
		return position;
	}

	public void position(long position) {
		this.position = position;
	}

	public int read(ByteBuffer data) throws IOException {
		int read = read(data, position);
		if (read != -1) {
			position += read;
		}
		return read;
	}

	public int read(ByteBuffer data, long offset) throws IOException {
		if (!channel.isOpen()) {
			throw new IllegalStateException("The channel is closed");
		}
		if (log.isTraceEnabled()) {
			log.trace("Client read request for {} bytes at virtual offset: 0x{}", data.limit(),
					Long.toHexString(offset));
		}

		return cluster_table.read(data, offset);
	}

	public int write(ByteBuffer data) {
		int write = write(data, position);
		if (write != -1) {
			position += write;
		}
		return write;
	}

	public int write(ByteBuffer data, long offset) {
		if (!channel.isOpen()) {
			throw new IllegalStateException("The channel is closed");
		}
		if (log.isTraceEnabled()) {
			log.trace("Client write request for {} bytes at virtual offset: 0x{}", data.limit(),
					Long.toHexString(offset));
		}

		return cluster_table.write(data, offset);
	}

}
