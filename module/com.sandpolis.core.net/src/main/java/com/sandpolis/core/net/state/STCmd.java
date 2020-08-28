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

import com.sandpolis.core.foundation.util.IDUtil;
import com.sandpolis.core.instance.state.DefaultCollection;
import com.sandpolis.core.instance.state.DefaultDocument;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.Oid.CollectionOid;
import com.sandpolis.core.instance.state.Oid.DocumentOid;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.net.msg.MsgState.RQ_STSnapshot;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection;
import com.sandpolis.core.net.msg.MsgState.RS_STSnapshot;
import com.sandpolis.core.net.msg.MsgState.RS_STSync;

public class STCmd extends Cmdlet<STCmd> {

	public static final class STSyncStruct {
		public List<Oid<?>> whitelist = new ArrayList<>();
		public List<Oid<?>> blacklist = new ArrayList<>();

		public int streamId = IDUtil.stream();
		public int updatePeriod;
		public STSyncDirection direction = STSyncDirection.DOWNSTREAM;
	}

	public static final class STSnapshotStruct {
		public List<Oid<?>> whitelist = new ArrayList<>();
		public List<Oid<?>> blacklist = new ArrayList<>();
	}

	public CompletionStage<EntangledCollection> sync(CollectionOid<?> oid) {
		return sync(oid, struct -> {
		});
	}

	public CompletionStage<EntangledCollection> sync(CollectionOid<?> oid, Consumer<STSyncStruct> configurator) {
		final var config = new STSyncStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();
		for (var o : config.blacklist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder() //
				.setBaseOid(oid.toString()) //
				.setUpdatePeriod(config.updatePeriod) //
				.setDirection(config.direction);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelistOid);

		return request(RS_STSync.class, rq).thenApply(rs -> {
			return new EntangledCollection(new DefaultCollection(null, rs.getCollection()), config);
		});
	}

	public CompletionStage<EntangledDocument> sync(DocumentOid<?> oid) {
		return sync(oid, struct -> {
		});
	}

	public CompletionStage<EntangledDocument> sync(DocumentOid<?> oid, Consumer<STSyncStruct> configurator) {
		final var config = new STSyncStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();
		for (var o : config.blacklist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder() //
				.setBaseOid(oid.toString()) //
				.setUpdatePeriod(config.updatePeriod) //
				.setDirection(config.direction);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelistOid);

		return request(RS_STSync.class, rq).thenApply(rs -> {
			return new EntangledDocument(new DefaultDocument(null, rs.getDocument()), config);
		});
	}

	public CompletionStage<DefaultCollection> snapshot(CollectionOid<?> oid) {
		return snapshot(oid, struct -> {
		});
	}

	public CompletionStage<DefaultCollection> snapshot(CollectionOid<?> oid, Consumer<STSnapshotStruct> configurator) {
		final var config = new STSnapshotStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();
		for (var o : config.blacklist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder() //
				.setBaseOid(oid.toString());

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelistOid);

		return request(RS_STSnapshot.class, rq).thenApply(rs -> {
			return new DefaultCollection(null, rs.getCollection());
		});
	}

	public CompletionStage<DefaultDocument> snapshot(DocumentOid<?> oid) {
		return snapshot(oid, struct -> {
		});
	}

	public CompletionStage<DefaultDocument> snapshot(DocumentOid<?> oid, Consumer<STSnapshotStruct> configurator) {
		final var config = new STSnapshotStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();
		for (var o : config.blacklist)
			if (!o.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder() //
				.setBaseOid(oid.toString());

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelistOid);

		return request(RS_STSnapshot.class, rq).thenApply(rs -> {
			return new DefaultDocument(null, rs.getDocument());
		});
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
}
