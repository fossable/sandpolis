package com.sandpolis.core.instance.state;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;

public abstract class AbstractSTDocument extends AbstractSTObject<ProtoDocument> implements STDocument {

	protected Map<Integer, STAttribute<?>> attributes;

	protected Map<Integer, STCollection> collections;

	protected Map<Integer, STDocument> documents;

	protected String id;

	protected AbstractSTObject<?> parent;

	@Override
	public synchronized <E> STAttribute<E> attribute(int tag) {
		STAttribute<?> attribute = getAttribute(tag);
		if (attribute == null) {
			attribute = newAttribute();
			setAttribute(tag, attribute);
		}
		return (STAttribute<E>) attribute;
	}

	@Override
	public synchronized Stream<STAttribute<?>> attributes() {
		return attributes.values().stream();
	}

	@Override
	public synchronized STCollection collection(int tag) {
		var collection = getCollection(tag);
		if (collection == null) {
			collection = newCollection();
			setCollection(tag, collection);
		}
		return collection;
	}

	@Override
	public synchronized Stream<STCollection> collections() {
		return collections.values().stream();
	}

	@Override
	public synchronized STDocument document(int tag) {
		var document = getDocument(tag);
		if (document == null) {
			document = newDocument();
			setDocument(tag, document);
		}
		return document;
	}

	@Override
	public synchronized Stream<STDocument> documents() {
		return documents.values().stream();
	}

	@Override
	public synchronized <E> STAttribute<E> getAttribute(int tag) {
		return (STAttribute<E>) attributes.get(tag);
	}

	@Override
	public synchronized STCollection getCollection(int tag) {
		return collections.get(tag);
	}

	public synchronized STDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
	public synchronized String getId() {
		if (id == null)
			id = UUID.randomUUID().toString();
		return id;
	}

	@Override
	public synchronized void merge(ProtoDocument snapshot) {
		for (var entry : snapshot.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : snapshot.getCollectionMap().entrySet()) {
			collection(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : snapshot.getAttributeMap().entrySet()) {
			attribute(entry.getKey()).merge(entry.getValue());
		}

		if (!snapshot.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !snapshot.containsDocument(entry.getKey()));
			collections.entrySet().removeIf(entry -> !snapshot.containsCollection(entry.getKey()));
			attributes.entrySet().removeIf(entry -> !snapshot.containsAttribute(entry.getKey()));
		}
	}

	@Override
	public synchronized AbstractSTObject parent() {
		return parent;
	}

	@Override
	public synchronized void setAttribute(int tag, STAttribute<?> attribute) {
		attributes.put(tag, attribute);
		attribute.setTag(tag);
	}

	@Override
	public synchronized void setCollection(int tag, STCollection collection) {
		var previous = collections.put(tag, collection);
		collection.setTag(tag);

		if (previous == null) {
			fireCollectionAddedEvent(this, collection);
		}
	}

	@Override
	public synchronized void setDocument(int tag, STDocument document) {
		var previous = documents.put(tag, document);
		document.setTag(tag);

		if (previous == null) {
			fireDocumentAddedEvent(this, document);
		}
	}

	@Override
	public synchronized ProtoDocument snapshot(RelativeOid<?>... oids) {
		if (oids.length == 0) {
			var snapshot = ProtoDocument.newBuilder().setPartial(false);
			documents.forEach((tag, document) -> {
				snapshot.putDocument(tag, document.snapshot());
			});
			collections.forEach((tag, collection) -> {
				snapshot.putCollection(tag, collection.snapshot());
			});
			attributes.forEach((tag, attribute) -> {
				snapshot.putAttribute(tag, attribute.snapshot());
			});
			return snapshot.build();
		} else {
			var snapshot = ProtoDocument.newBuilder().setPartial(true);
			for (var head : Arrays.stream(oids).mapToInt(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				if (documents.containsKey(head))
					snapshot.putDocument(head, documents.get(head).snapshot(children));
				if (collections.containsKey(head))
					snapshot.putCollection(head, collections.get(head).snapshot(children));
				if (attributes.containsKey(head))
					snapshot.putAttribute(head, attributes.get(head).snapshot());
			}

			return snapshot.build();
		}
	}

	protected abstract STAttribute<?> newAttribute();

	protected abstract STCollection newCollection();

	protected abstract STDocument newDocument();
}
