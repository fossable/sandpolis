package com.sandpolis.core.instance.state.st;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralRelation;
import com.sandpolis.core.instance.state.vst.VirtObject;

public abstract class AbstractSTCollection extends AbstractSTObject<ProtoCollection> implements STCollection {

	protected BiFunction<STCollection, Long, STDocument> documentConstructor = EphemeralDocument::new;

	protected Map<Long, STDocument> documents;

	public AbstractSTCollection(STObject<?> parent, long id) {
		super(parent, id);
	}

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
	public List<STDocument> documents() {
		return List.copyOf(documents.values());
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		documents.values().forEach(consumer);
	}

	@Override
	public STDocument getDocument(long tag) {
		return documents.get(tag);
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	@Override
	public void merge(ProtoCollection snapshot) {
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
	public int size() {
		return documents.size();
	}

	@Override
	public ProtoCollection snapshot(RelativeOid... oids) {
		var snapshot = ProtoCollection.newBuilder().setTag(oid.last());
		if (oids.length == 0) {

			synchronized (documents) {
				documents.values().stream().map(STDocument::snapshot).forEach(snapshot::addDocument);
			}

		} else {
			for (var head : Arrays.stream(oids).mapToLong(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				snapshot.addDocument(documents.get(head).snapshot(children));
			}
		}

		return snapshot.build();
	}
}
