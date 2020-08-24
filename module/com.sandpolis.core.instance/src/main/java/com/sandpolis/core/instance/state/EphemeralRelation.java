package com.sandpolis.core.instance.state;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class EphemeralRelation<T extends VirtObject> implements STRelation<T> {

	private Map<Integer, STDocument> documents;

	private Function<STDocument, T> constructor;

	public EphemeralRelation(Function<STDocument, T> constructor) {
		this.constructor = constructor;
	}

	public void add(T element) {
		documents.put(element.tag(), element.document);
	}

	public Stream<T> stream() {
		return documents.values().stream().map(constructor);
	}

	public int size() {
		return documents.size();
	}

	public boolean contains(T element) {
		return documents.containsKey(element.tag());
	}

}
