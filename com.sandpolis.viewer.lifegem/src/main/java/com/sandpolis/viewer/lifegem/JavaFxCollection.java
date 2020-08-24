package com.sandpolis.viewer.lifegem;

import java.util.Map;
import java.util.function.Function;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;

import javafx.collections.ObservableListBase;

public class JavaFxCollection<T extends VirtObject> extends ObservableListBase<T> implements STCollection {

	private Map<Integer, JavaFxDocument> documents;

	@Override
	public T get(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		return documents.size();
	}

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
