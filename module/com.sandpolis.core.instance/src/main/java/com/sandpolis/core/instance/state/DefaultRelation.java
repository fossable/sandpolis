package com.sandpolis.core.instance.state;

import java.util.function.Function;
import java.util.stream.Stream;

public class DefaultRelation<T extends VirtObject> implements STRelation<T> {

	public DefaultRelation(Function<STDocument, T> constructor) {
	}

	@Override
	public void add(T element) {
		// TODO Auto-generated method stub

	}

	@Override
	public Stream<T> stream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean contains(T element) {
		// TODO Auto-generated method stub
		return false;
	}

}
