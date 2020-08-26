package com.sandpolis.core.net.state;

import com.sandpolis.core.instance.state.DefaultCollection;
import com.sandpolis.core.net.stream.Stream;

public class EntangledCollection extends DefaultCollection {

	private Stream stream;

	public EntangledCollection() {

	}

	public Stream stream() {
		return stream;
	}

}
