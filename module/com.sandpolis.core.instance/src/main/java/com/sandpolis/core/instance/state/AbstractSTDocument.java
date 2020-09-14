package com.sandpolis.core.instance.state;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;

public abstract class AbstractSTDocument extends AbstractSTObject implements STDocument {

	protected String id;

	protected AbstractSTObject parent;

	protected Map<Integer, STDocument> documents;

	protected Map<Integer, STCollection> collections;

	protected Map<Integer, STAttribute<?>> attributes;

	@Override
	public <E> STAttribute<E> attribute(int tag) {
		STAttribute<?> attribute = getAttribute(tag);
		if (attribute == null) {
			attribute = newAttribute();
			setAttribute(tag, attribute);
		}
		return (STAttribute<E>) attribute;
	}

	@Override
	public <E> STAttribute<E> getAttribute(int tag) {
		return (STAttribute<E>) attributes.get(tag);
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		attributes.put(tag, attribute);
		attribute.setTag(tag);
	}

	@Override
	public STDocument document(int tag) {
		var document = getDocument(tag);
		if (document == null) {
			document = newDocument();
			setDocument(tag, document);
		}
		return document;
	}

	public STDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		documents.put(tag, document);
		document.setTag(tag);
	}

	@Override
	public STCollection collection(int tag) {
		var collection = getCollection(tag);
		if (collection == null) {
			collection = newCollection();
			setCollection(tag, collection);
		}
		return collection;
	}

	@Override
	public STCollection getCollection(int tag) {
		return collections.get(tag);
	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		collections.put(tag, collection);
		collection.setTag(tag);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void merge(ProtoDocument snapshot) {
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
	public ProtoDocument snapshot(RelativeOid<?>... oids) {
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

	protected abstract STDocument newDocument();

	protected abstract STCollection newCollection();

	@Override
	public Stream<STAttribute<?>> attributes() {
		return attributes.values().stream();
	}

	@Override
	public Stream<STCollection> collections() {
		return collections.values().stream();
	}

	@Override
	public Stream<STDocument> documents() {
		return documents.values().stream();
	}

	@Override
	public AbstractSTObject parent() {
		return parent;
	}
}
