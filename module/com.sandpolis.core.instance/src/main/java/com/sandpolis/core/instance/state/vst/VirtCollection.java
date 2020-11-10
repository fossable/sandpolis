package com.sandpolis.core.instance.state.vst;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.sandpolis.core.foundation.util.RandUtil;
import com.sandpolis.core.instance.state.st.STDocument;

public class VirtCollection<V extends VirtDocument> implements VirtObject {

	protected Map<String, V> documents;

	private STDocument collection;

	public VirtCollection(STDocument collection) {
		this.collection = collection;
	}

	public V add(Function<STDocument, V> constructor) {
		String id = "" + RandUtil.nextLong();

		var object = constructor.apply(collection.document(id));
		documents.put(id, object);
		return object;
	}

	public long count() {
		return documents.size();
	}

	public Optional<V> get(String path) {
		return Optional.ofNullable(documents.get(path));
	}
}
