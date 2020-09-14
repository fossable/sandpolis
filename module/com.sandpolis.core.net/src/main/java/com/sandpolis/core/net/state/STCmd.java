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
package com.sandpolis.core.net.state;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.foundation.util.IDUtil;
import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.EphemeralCollection;
import com.sandpolis.core.instance.state.EphemeralDocument;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.STCollectionOid;
import com.sandpolis.core.instance.state.oid.STDocumentOid;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.msg.MsgState.OidType;
import com.sandpolis.core.net.msg.MsgState.RQ_STSnapshot;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection;

public class STCmd extends Cmdlet<STCmd> {

	@ConfigStruct
	public static final class STSnapshotStruct {
		public List<Oid> whitelist = new ArrayList<>();
	}

	@ConfigStruct
	public static final class STSyncStruct {
		public Connection connection;

		public STSyncDirection direction = STSyncDirection.DOWNSTREAM;
		public boolean initiator;
		public int streamId = IDUtil.stream();
		public int updatePeriod;
		public List<Oid> whitelist = new ArrayList<>();
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

	public CompletionStage<EphemeralCollection> snapshot(STCollectionOid<?> oid) {
		return snapshot(oid, struct -> {
		});
	}

	public CompletionStage<EphemeralCollection> snapshot(STCollectionOid<?> oid,
			Consumer<STSnapshotStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required");

		final var config = new STSnapshotStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder() //
				.setOid(oid.toString()) //
				.setOidType(OidType.COLLECTION);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		return request(ProtoCollection.class, rq).thenApply(rs -> {
			return new EphemeralCollection(null, rs);
		});
	}

	public CompletionStage<EphemeralDocument> snapshot(STDocumentOid<?> oid) {
		return snapshot(oid, struct -> {
		});
	}

	public CompletionStage<EphemeralDocument> snapshot(STDocumentOid<?> oid, Consumer<STSnapshotStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required");

		final var config = new STSnapshotStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder() //
				.setOid(oid.toString()) //
				.setOidType(OidType.DOCUMENT);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		return request(ProtoDocument.class, rq).thenApply(rs -> {
			return new EphemeralDocument((STDocument) null, rs);
		});
	}

	public CompletionStage<EntangledCollection> sync(STCollection collection, STCollectionOid<?> oid) {
		return sync(collection, oid, struct -> {
		});
	}

	public CompletionStage<EntangledCollection> sync(STCollection collection, STCollectionOid<?> oid,
			Consumer<STSyncStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required");

		final var config = new STSyncStruct();
		config.connection = target;
		config.initiator = true;
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder() //
				.setStreamId(config.streamId) //
				.setOid(oid.toString()) //
				.setOidType(OidType.COLLECTION) //
				.setUpdatePeriod(config.updatePeriod) //
				.setDirection(config.direction);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		var entangled = new EntangledCollection(collection, config);

		return request(Outcome.class, rq).thenApply(rs -> {
			return entangled;
		});
	}

	public CompletionStage<EntangledDocument> sync(STDocumentOid<?> oid) {
		return sync(oid, struct -> {
		});
	}

	public CompletionStage<EntangledDocument> sync(STDocumentOid<?> oid, Consumer<STSyncStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required");

		final var config = new STSyncStruct();
		config.initiator = true;
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder() //
				.setStreamId(config.streamId) //
				.setOid(oid.toString()) //
				.setOidType(OidType.DOCUMENT) //
				.setUpdatePeriod(config.updatePeriod) //
				.setDirection(config.direction);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		var document = new EntangledDocument(new EphemeralDocument((STDocument) null), config);

		return request(Outcome.class, rq).thenApply(rs -> {
			return document;
		});
	}
}
