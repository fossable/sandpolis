//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state;

import static org.s7s.core.instance.state.STStore.STStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.foundation.S7SRandom;
import org.s7s.core.protocol.Stream.RQ_STStream;
import org.s7s.core.protocol.Stream.RS_STStream;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.cmdlet.Cmdlet;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.instance.state.st.entangled.EntangledDocument;

public class STCmd extends Cmdlet<STCmd> {

	private static final Logger log = LoggerFactory.getLogger(STCmd.class);

	public static final class STSnapshotStruct {
		public final List<Oid> whitelist = new ArrayList<>();

		private STSnapshotStruct(Consumer<STSnapshotStruct> configurator) {
			configurator.accept(this);
		}
	}

	public static final class STSyncStruct {
		public Connection connection;

		public RQ_STStream.Direction direction = RQ_STStream.Direction.DOWNSTREAM;
		public boolean initiator;
		public int streamId = S7SRandom.nextNonzeroInt();
		public int updatePeriod;
		public List<Oid> whitelist = new ArrayList<>();
		public boolean permanent = true;

		public STSyncStruct(Consumer<STSyncStruct> configurator) {
			configurator.accept(this);

			if (streamId == 0) {
				throw new RuntimeException("Invalid stream ID");
			}
		}
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link STCmd} can be invoked
	 */
	public static STCmd async() {
		return new STCmd();
	}

	private STCmd() {
	}

	public CompletionStage<STDocument> snapshot(Oid oid) {
		return snapshot(oid, struct -> {
		});
	}

	public CompletionStage<STDocument> snapshot(Oid oid, Consumer<STSnapshotStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required");

		var config = new STSnapshotStruct(configurator);

		for (var o : config.whitelist)
			if (!oid.isAncestorOf(o))
				throw new IllegalArgumentException();

		int id = S7SRandom.nextNonzeroInt();// Not in closure

		return sync(oid, sync_config -> {
			sync_config.connection = target;
			sync_config.initiator = true;
			sync_config.permanent = false;
			sync_config.whitelist = config.whitelist;
			sync_config.streamId = id;
		}).thenApply(document -> {
			try {
				document.getInactiveFuture().get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return document;
		});
	}

	public CompletionStage<EntangledDocument> sync(Oid oid) {

		int id = S7SRandom.nextNonzeroInt();// Not in closure

		return sync(oid, config -> {
			config.connection = target;
			config.initiator = true;
			config.streamId = id;
		});
	}

	public CompletionStage<EntangledDocument> sync(Oid oid, Consumer<STSyncStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required (" + oid + ")");
		if (oid.path().length == 0)
			throw new IllegalArgumentException("Empty OID");

		var config = new STSyncStruct(configurator);

		if (!config.initiator) {
			throw new IllegalArgumentException();
		}

		for (var o : config.whitelist)
			if (!oid.isAncestorOf(o))
				throw new IllegalArgumentException();

		var rq = RQ_STStream.newBuilder() //
				.setStreamId(config.streamId) //
				.setOid(oid.toString()) //
				.setUpdatePeriod(config.updatePeriod) //
				.setPermanent(config.permanent) //
				.setDirection(config.direction);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		var document = new EntangledDocument(STStore.get(oid), configurator);

		log.debug("Sending sync command for OID: {}", oid);
		return request(RS_STStream.class, rq).thenApply(rs -> {
			return document;
		});
	}
}
