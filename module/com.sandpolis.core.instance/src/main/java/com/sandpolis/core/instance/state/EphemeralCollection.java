package com.sandpolis.core.instance.state;

import java.util.function.Function;

import com.sandpolis.core.instance.State.ProtoCollection;

public class EphemeralCollection implements STCollection {

	@Override
	public void merge(ProtoCollection snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoCollection snapshot(Oid<?>... oids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends VirtObject> STRelation<T> collectionList(Function<STDocument, T> constructor) {
		// TODO Auto-generated method stub
		return null;
	}

}
