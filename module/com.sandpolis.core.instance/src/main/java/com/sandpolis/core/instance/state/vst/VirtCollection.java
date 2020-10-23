package com.sandpolis.core.instance.state.vst;

import java.util.Map;
import java.util.Optional;

import com.sandpolis.core.foundation.Result.ErrorCode;

public class VirtCollection<V extends VirtDocument> implements VirtObject {

	protected Map<Long, V> documents;

	public void add(V object) {
		if (object.complete() != ErrorCode.OK)
			throw new IncompleteObjectException();

		documents.put(object.oid().last(), object);
	}

	public long count() {
		return documents.size();
	}

	public Optional<V> get(long tag) {
		return Optional.ofNullable(documents.get(tag));
	}
}
