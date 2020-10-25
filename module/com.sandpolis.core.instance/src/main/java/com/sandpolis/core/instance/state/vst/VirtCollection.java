package com.sandpolis.core.instance.state.vst;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.sandpolis.core.foundation.util.RandUtil;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;

public class VirtCollection<V extends VirtDocument> implements VirtObject {

	protected Map<Long, V> documents;

	private STCollection collection;

	public VirtCollection(STCollection collection) {
		this.collection = collection;
	}

	public V add(Function<STDocument, V> constructor) {
		long id = RandUtil.nextLong();

		var object = constructor.apply(collection.document(id));
		documents.put(id, object);
		return object;
	}

	public long count() {
		return documents.size();
	}

	public Optional<V> get(long tag) {
		return Optional.ofNullable(documents.get(tag));
	}
}
