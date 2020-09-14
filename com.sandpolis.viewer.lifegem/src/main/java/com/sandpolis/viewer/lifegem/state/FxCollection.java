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
package com.sandpolis.viewer.lifegem.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.EphemeralDocument;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.store.StoreMetadata;

import javafx.application.Platform;
import javafx.collections.ObservableListBase;

public class FxCollection<T extends VirtObject> extends ObservableListBase<T> implements STCollection {

	private List<T> container;

	private Function<STDocument, T> constructor;

	public FxCollection(Function<STDocument, T> constructor) {
		this.container = new ArrayList<>();
		this.constructor = Objects.requireNonNull(constructor);
	}

	public FxCollection(STCollection base, Function<STDocument, T> constructor) {
		this(constructor);
		base.documents().map(constructor).forEach(container::add);
	}

	@Override
	public void addListener(Object listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public STDocument document(int tag) {
		for (var item : container) {
			if (item.tag() == tag) {
				return item.document;
			}
		}

		var document = newDocument();

		Platform.runLater(() -> {
			System.out.println("Adding document tag: " + tag + " at: " + container.size());
			container.add(constructor.apply(document));
			System.out.println("size: " + container.size());
		});

		return document;
	}

	@Override
	public Stream<STDocument> documents() {
		return stream().map(item -> item.document);
	}

	@Override
	public T get(int index) {
		return container.get(index);
	}

	@Override
	public STDocument getDocument(int tag) {
		for (var item : container) {
			if (item.tag() == tag) {
				return item.document;
			}
		}
		return null;
	}

	@Override
	public StoreMetadata getMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void merge(ProtoCollection snapshot) {
		System.out.println("Merging snapshot: " + snapshot);
		for (var entry : snapshot.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}

//		if (!snapshot.getPartial()) {
//			Platform.runLater(() -> {
//				// Remove anything that wasn't in the snapshot
//				container.removeIf(item -> !snapshot.containsDocument(item.tag()));
//			});
//		}
	}

	@Override
	public STDocument newDocument() {
		return new FxDocument<T>(null);
	}

	@Override
	public Oid oid() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeListener(Object listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		Platform.runLater(() -> {
			for (int i = 0; i < container.size(); i++) {
				if (container.get(i).tag() == tag) {
					container.set(i, constructor.apply(document));
					return;
				}
			}
		});
	}

	@Override
	public void setTag(int tag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		System.out.println("Container: " + container);
		return container.size();
	}

	@Override
	public ProtoCollection snapshot(RelativeOid<?>... oids) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Stream<T> stream() {
		return container.stream();
	}
}
