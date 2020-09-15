package com.sandpolis.core.instance.state;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;

public abstract class AbstractSTCollection extends AbstractSTObject implements STCollection {

	protected Map<Integer, STDocument> documents;

	protected STDocument parent;

	public void clear() {
		documents.clear();
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		return new EphemeralRelation<>(constructor);
	}

	public boolean contains(STDocument document) {
		return documents.containsValue(document);
	}

	@Override
	public STDocument document(int tag) {
		var document = getDocument(tag);
		if (document == null) {
			document = new EphemeralDocument(this);
			setDocument(tag, document);
		}
		return document;
	}

	@Override
	public Stream<STDocument> documents() {
		return documents.values().stream();
	}

	public STDocument get(int key) {
		return documents.get(key);
	}

	@Override
	public STDocument getDocument(int tag) {
		return documents.get(tag);
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	@Override
	public void merge(ProtoCollection snapshot) {
		for (var entry : snapshot.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}

		if (!snapshot.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !snapshot.containsDocument(entry.getKey()));
		}
	}

	@Override
	public STDocument newDocument() {
		return new EphemeralDocument(this);
	}

	@Override
	public AbstractSTObject parent() {
		return (AbstractSTObject) parent;
	}

	@Override
	public void remove(STDocument document) {
		documents.values().remove(document);
		fireCollectionRemovedEvent(this, document);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		documents.put(tag, document);
		document.setTag(tag);
	}

	@Override
	public int size() {
		return documents.size();
	}

	@Override
	public ProtoCollection snapshot(RelativeOid<?>... oids) {
		if (oids.length == 0) {
			var snapshot = ProtoCollection.newBuilder().setPartial(false);
			documents.forEach((tag, document) -> {
				snapshot.putDocument(tag, document.snapshot());
			});
			return snapshot.build();
		} else {
			var snapshot = ProtoCollection.newBuilder().setPartial(true);
			for (var head : Arrays.stream(oids).mapToInt(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				snapshot.putDocument(head, documents.get(head).snapshot(children));
			}

			return snapshot.build();
		}
	}
}
