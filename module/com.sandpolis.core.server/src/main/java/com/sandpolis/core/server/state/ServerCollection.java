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
package com.sandpolis.core.server.state;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.EphemeralRelation;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.OidConverter;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;

@Entity
public class ServerCollection implements STCollection {

	@Id
	private String db_id;

	@Column
	@Convert(converter = OidConverter.class)
	private Oid<?> oid;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, ServerDocument> documents;

	public ServerCollection(Oid<?> oid) {
		this.oid = oid;
		this.db_id = UUID.randomUUID().toString();
		this.documents = new HashMap<>();
	}

	public ServerCollection(Oid<?> oid, ProtoCollection collection) {
		this(oid);
		merge(collection);
	}

	protected ServerCollection() {
		// JPA CONSTRUCTOR
	}

	public Oid<?> getOid() {
		return oid;
	}

	public ServerDocument get(int key) {
		return documents.get(key);
	}

	public Stream<ServerDocument> stream() {
		return documents.values().stream();
	}

	@Override
	public <T extends VirtObject> STRelation<T> collectionList(Function<STDocument, T> constructor) {
		return new EphemeralRelation<>(constructor);
	}

	public int size() {
		return documents.size();
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	public boolean contains(STDocument document) {
		return documents.containsValue(document);
	}

	public void add(int tag, ServerDocument e) {
		documents.put(tag, e);
	}

	public boolean remove(STDocument document) {
		return documents.values().remove(document);
	}

	public void clear() {
		documents.clear();
	}

	public ServerDocument document(int tag) {
		ServerDocument document = documents.get(tag);
		if (document == null) {
			document = new ServerDocument(oid.child(tag));
			documents.put(tag, document);
		}
		return document;
	}

	@Override
	public void merge(ProtoCollection delta) {
		for (var entry : delta.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}

		if (!delta.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !delta.containsDocument(entry.getKey()));
		}
	}

	@Override
	public ProtoCollection snapshot(Oid<?>... oids) {
		if (oids.length == 0) {
			var snapshot = ProtoCollection.newBuilder().setPartial(false);
			documents.forEach((tag, document) -> {
				snapshot.putDocument(tag, document.snapshot());
			});
			return snapshot.build();
		} else {
			var snapshot = ProtoCollection.newBuilder().setPartial(true);
			for (var head : Arrays.stream(oids).mapToInt(Oid::head).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.head() != head).map(Oid::tail).toArray(Oid[]::new);

				snapshot.putDocument(head, documents.get(head).snapshot(children));
			}

			return snapshot.build();
		}
	}
}
