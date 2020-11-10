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
package com.sandpolis.client.lifegem.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.st.AbstractSTDocument;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.vst.VirtObject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FxDocument<T extends VirtObject> extends AbstractSTDocument {

	private Function<STDocument, T> constructor;

	private ObservableList<T> container;

	private Map<Long, Integer> indexMap;

	public FxDocument(STDocument parent, Function<STDocument, T> constructor) {
		super(parent, AbsoluteOid.ROOT);
		this.container = FXCollections.observableArrayList();
		this.indexMap = new HashMap<>();
		this.constructor = Objects.requireNonNull(constructor);
	}

	public FxDocument(STDocument parent, STDocument base, Function<STDocument, T> constructor) {
		this(parent, constructor);
		base.documents().stream().map(constructor).forEach(container::add);
	}

	@Override
	public STDocument document(RelativeOid<STDocument> oid) {
//		var index = indexMap.get(tag);
//		if (index != null)
//			return container.get(index).document;
//
//		var document = newDocument();
//		setDocument(tag, document);
//
//		return document;
		return null;
	}

	@Override
	public List<STDocument> documents() {
//		return container.stream().map(item -> item.document).collect(Collectors.toList());
		return null;
	}

	@Override
	public STDocument getDocument(RelativeOid<STDocument> oid) {
//		if (indexMap.containsKey(tag))
//			return container.get(indexMap.get(tag)).document;

		return null;
	}

//	@Override
//	public StoreMetadata getMetadata() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public ObservableList<T> getObservable() {
		return container;
	}

	@Override
	public void merge(ProtoDocument snapshot) {
		synchronized (container) {
			for (var document : snapshot.getDocumentList()) {
				if (document.getRemoval()) {
					// TODO
					continue;
				} else if (document.getReplacement()) {
					// TODO
				}
				document(document.getPath()).merge(document);
			}
		}
	}

	@Override
	public void remove(STDocument document) {
//		for (int i = 0; i < container.size(); i++) {
//			if (container.get(i).document.equals(document)) {
//				final int index = i;
//				indexMap.values().removeIf(j -> j == index);
//				container.remove(index);
//				return;
//			}
//		}
	}

//	@Override
//	public void setDocument(long tag, STDocument document) {
//		Platform.runLater(() -> {
//			var index = indexMap.get(tag);
//			if (index == null) {
//				indexMap.put(tag, container.size());
//				container.add(constructor.apply(document));
//			} else {
//				container.set(index, constructor.apply(document));
//			}
//		});
//	}

	@Override
	public ProtoDocument snapshot(RelativeOid... oids) {
		throw new UnsupportedOperationException();
	}

}
