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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import com.sandpolis.core.instance.State.RQ_STSnapshot;
import com.sandpolis.core.instance.State.RQ_STSync;
import com.sandpolis.core.instance.State.RS_STSnapshot;
import com.sandpolis.core.instance.state.DefaultCollection;
import com.sandpolis.core.instance.state.DefaultDocument;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.Oid.CollectionOid;
import com.sandpolis.core.instance.state.Oid.DocumentOid;
import com.sandpolis.core.net.cmdlet.Cmdlet;

public class STCmd extends Cmdlet<STCmd> {

	public static final class STSyncStruct {
		public List<Oid<?>> whitelist = new ArrayList<>();
		public List<Oid<?>> blacklist = new ArrayList<>();

		public int updatePeriod;
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
		for (var a : attributes)
			if (!a.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder().setBaseOid(oid.toString())
				.addAllWhitelistOid((Iterable<String>) Arrays.stream(attributes).map(Oid::toString));

		return null;
	}

	public CompletionStage<EntangledDocument> sync(DocumentOid<?> oid, Consumer<STSyncStruct> configurator) {
		for (var a : attributes)
			if (!a.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder().setBaseOid(oid.toString())
				.addAllWhitelistOid((Iterable<String>) Arrays.stream(attributes).map(Oid::toString));

		return null;
	}

	public CompletionStage<DefaultCollection> snapshot(CollectionOid<?> oid, Consumer<STSnapshotStruct> configurator) {
		for (var a : attributes)
			if (!a.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder().setBaseOid(oid.toString())
				.addAllWhitelistOid((Iterable<String>) Arrays.stream(attributes).map(Oid::toString));

		return request(RS_STSnapshot.class, rq).thenApply(rs -> {
			return new DefaultCollection(null, rs.getCollection());
		});
	}

	public CompletionStage<DefaultDocument> snapshot(DocumentOid<?> oid, Consumer<STSnapshotStruct> configurator) {
		for (var a : attributes)
			if (!a.isChildOf(oid))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder().setBaseOid(oid.toString())
				.addAllWhitelistOid((Iterable<String>) Arrays.stream(attributes).map(Oid::toString));

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
