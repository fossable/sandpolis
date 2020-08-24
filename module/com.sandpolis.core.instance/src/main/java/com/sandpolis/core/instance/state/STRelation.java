package com.sandpolis.core.instance.state;

import java.util.Iterator;
import java.util.stream.Stream;

public interface STRelation<T extends VirtObject> extends Iterable<T> {

	public void add(T element);

	public Stream<T> stream();

	public int size();

	public boolean contains(T element);

	@Override
	public default Iterator<T> iterator() {
		return stream().iterator();
	}
}
