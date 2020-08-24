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

public class StateTreeCmd extends Cmdlet<StateTreeCmd> {

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
	 *         commands in {@link StateTreeCmd} can be invoked
	 */
	public static StateTreeCmd async() {
		return new StateTreeCmd();
	}

	private StateTreeCmd() {
	}
}
