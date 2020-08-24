package com.sandpolis.viewer.lifegem;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.VirtObject;

public class JavaFxDocument<T extends VirtObject> implements STDocument {

	@Override
	public void merge(ProtoDocument snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoDocument snapshot(Oid<?>... oids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E> STAttribute<E> attribute(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STCollection collection(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument document(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E> STAttribute<E> getAttribute(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STCollection getCollection(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument getDocument(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Oid<?> getOid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDocument(int tag, STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOid(Oid<?> oid) {
		// TODO Auto-generated method stub

	}

}
