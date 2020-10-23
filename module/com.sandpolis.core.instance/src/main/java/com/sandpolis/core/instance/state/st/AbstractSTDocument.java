package com.sandpolis.core.instance.state.st;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralAttribute;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralCollection;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;

public abstract class AbstractSTDocument extends AbstractSTObject<ProtoDocument> implements STDocument {

	protected BiFunction<STDocument, Long, STAttribute<?>> attributeConstructor = EphemeralAttribute::new;

	protected Map<Long, STAttribute<?>> attributes;

	protected BiFunction<STDocument, Long, STCollection> collectionConstructor = EphemeralCollection::new;

	protected Map<Long, STCollection> collections;

	protected BiFunction<STDocument, Long, STDocument> documentConstructor = EphemeralDocument::new;

	protected Map<Long, STDocument> documents;

	protected String id;

	public AbstractSTDocument(STObject<?> parent, long id) {
		super(parent, id);
	}

	@Override
	public <E> STAttribute<E> attribute(long tag) {
		STAttribute<?> attribute = getAttribute(tag);
		if (attribute == null) {
			attribute = attributeConstructor.apply(this, tag);
			synchronized (attributes) {
				var previous = attributes.put(tag, attribute);

//				if (previous == null) {
//					fireAttributeAddedEvent(this, attribute);
//				}
			}
		}
		return (STAttribute<E>) attribute;
	}

	@Override
	public Collection<STAttribute<?>> attributes() {
		return Collections.unmodifiableCollection(attributes.values());
	}

	@Override
	public STCollection collection(long tag) {
		var collection = getCollection(tag);
		if (collection == null) {
			collection = collectionConstructor.apply(this, tag);
			synchronized (collections) {
				var previous = collections.put(tag, collection);

				if (previous == null) {
					fireCollectionAddedEvent(this, collection);
				}
			}
		}
		return collection;
	}

	@Override
	public Collection<STCollection> collections() {
		return Collections.unmodifiableCollection(collections.values());
	}

	@Override
	public STDocument document(long tag) {
		var document = getDocument(tag);
		if (document == null) {
			document = documentConstructor.apply(this, tag);
			synchronized (documents) {
				var previous = documents.put(tag, document);

				if (previous == null) {
					fireDocumentAddedEvent(this, document);
				}
			}
		}
		return document;
	}

	@Override
	public Collection<STDocument> documents() {
		return Collections.unmodifiableCollection(documents.values());
	}

	@Override
	public void forEachAttribute(Consumer<STAttribute<?>> consumer) {
		attributes.values().forEach(consumer);
	}

	@Override
	public void forEachCollection(Consumer<STCollection> consumer) {
		collections.values().forEach(consumer);
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		documents.values().forEach(consumer);
	}

	@Override
	public <E> STAttribute<E> getAttribute(long tag) {
		return (STAttribute<E>) attributes.get(tag);
	}

	@Override
	public STCollection getCollection(long tag) {
		return collections.get(tag);
	}

	public STDocument getDocument(long tag) {
		return documents.get(tag);
	}

	@Override
	public String getId() {
		if (id == null)
			id = UUID.randomUUID().toString();
		return id;
	}

	@Override
	public void merge(ProtoDocument snapshot) {
		synchronized (documents) {
			for (var document : snapshot.getDocumentList()) {
				if (document.getRemoval()) {
					var removal = documents.remove(document.getTag());
					if (removal != null) {
						fireDocumentRemovedEvent(this, removal);
					}
					continue;
				} else if (document.getReplacement()) {
					documents.remove(document.getTag());
				}
				document(document.getTag()).merge(document);
			}
		}

		synchronized (collections) {
			for (var collection : snapshot.getCollectionList()) {
				if (collection.getRemoval()) {
					var removal = collections.remove(collection.getTag());
					if (removal != null) {
						fireCollectionRemovedEvent(this, removal);
					}
					continue;
				} else if (collection.getReplacement()) {
					collections.remove(collection.getTag());
				}
				collection(collection.getTag()).merge(collection);
			}
		}

		synchronized (attributes) {
			for (var attribute : snapshot.getAttributeList()) {
				if (attribute.getRemoval()) {
					attributes.remove(attribute.getTag());
					continue;
				} else if (attribute.getReplacement()) {
					attributes.remove(attribute.getTag());
				}
				attribute(attribute.getTag()).merge(attribute);
			}
		}
	}

	@Override
	public void remove(STAttribute<?> attribute) {
		synchronized (attributes) {
			if (attributes.values().remove(attribute)) {
//				fireAttributeRemovedEvent(this, attribute);
			}
		}
	}

	@Override
	public void remove(STCollection collection) {
		synchronized (collections) {
			if (collections.values().remove(collection)) {
				fireCollectionRemovedEvent(this, collection);
			}
		}
	}

	@Override
	public void remove(STDocument document) {
		synchronized (documents) {
			if (documents.values().remove(document)) {
				fireDocumentRemovedEvent(this, document);
			}
		}
	}

	@Override
	public ProtoDocument snapshot(RelativeOid... oids) {
		var snapshot = ProtoDocument.newBuilder().setTag(oid.last());

		if (oids.length == 0) {
			synchronized (documents) {
				documents.values().stream().map(STDocument::snapshot).forEach(snapshot::addDocument);
			}
			synchronized (collections) {
				collections.values().stream().map(STCollection::snapshot).forEach(snapshot::addCollection);
			}
			synchronized (attributes) {
				attributes.values().stream().map(STAttribute::snapshot).forEach(snapshot::addAttribute);
			}
		} else {
			for (var head : Arrays.stream(oids).mapToLong(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				if (documents.containsKey(head))
					snapshot.addDocument(documents.get(head).snapshot(children));
				if (collections.containsKey(head))
					snapshot.addCollection(collections.get(head).snapshot(children));
				if (attributes.containsKey(head))
					snapshot.addAttribute(attributes.get(head).snapshot());
			}
		}

		return snapshot.build();
	}
}
