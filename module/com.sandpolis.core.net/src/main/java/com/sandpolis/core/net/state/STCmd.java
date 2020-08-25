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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sandpolis.core.instance.State.RQ_STSnapshot;
import com.sandpolis.core.instance.State.RS_STSnapshot;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.Oid.CollectionOid;
import com.sandpolis.core.instance.state.Oid.DocumentOid;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STStore;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.net.cmdlet.Cmdlet;

public class STCmd extends Cmdlet<STCmd> {

	public CompletionStage<Void> sync(Oid oid, Oid... attributes) {
		return null;
	}

	public <E extends VirtObject> CompletionStage<Map<Integer, E>> snapshot(CollectionOid<?> oid,
			Function<STDocument, E> constructor, Oid<?>... attributes) {

		var rq = RQ_STSnapshot.newBuilder().setBaseOid(oid.toString())
				.addAllWhitelistOid((Iterable<String>) Arrays.stream(attributes).map(Oid::toString));

		return request(RS_STSnapshot.class, rq).thenApply(rs -> {
			return rs.getCollection().getDocumentMap().entrySet().stream().collect(Collectors.toMap(
					entry -> entry.getKey(), entry -> constructor.apply(STStore.newRootDocument(entry.getValue()))));
		});
	}

	public <E extends VirtObject> CompletionStage<E> snapshot(DocumentOid<?> oid, Function<STDocument, E> constructor,
			Oid<?>... attributes) {

		var rq = RQ_STSnapshot.newBuilder().setBaseOid(oid.toString())
				.addAllWhitelistOid((Iterable<String>) Arrays.stream(attributes).map(Oid::toString));

		return request(RS_STSnapshot.class, rq).thenApply(rs -> {
			return constructor.apply(STStore.newRootDocument(rs.getDocument()));
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
