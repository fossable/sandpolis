package com.sandpolis.core.net.state;


import static com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection.BIDIRECTIONAL;
import static com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection.DOWNSTREAM;
import static com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection.UPSTREAM;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

public class EntangledCollection implements STCollection {

	private STCollection container;

	private StreamSink<ProtoCollection> sink;
	private StreamSource<ProtoCollection> source;

	public EntangledCollection(STCollection container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		if (config.direction == DOWNSTREAM || config.direction == BIDIRECTIONAL) {
			sink = new StreamSink<>() {

				@Override
				public void onNext(ProtoCollection item) {
					container.merge(item);
				};
			};
		}

		if (config.direction == UPSTREAM || config.direction == BIDIRECTIONAL) {
			source = new StreamSource<>() {

				@Override
				public void start() {
					// TODO Auto-generated method stub

				}

				@Override
				public void stop() {
					// TODO Auto-generated method stub

				}
			};
		}
	}

	// Begin boilerplate

	@Override
	public void merge(ProtoCollection snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoCollection snapshot(Oid<?>... oids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument document(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STDocument> documents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument getDocument(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		// TODO Auto-generated method stub
		return null;
	}

}
