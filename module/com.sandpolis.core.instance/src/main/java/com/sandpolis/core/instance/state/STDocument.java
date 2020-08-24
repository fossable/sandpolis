package com.sandpolis.core.instance.state;

import com.sandpolis.core.instance.State.ProtoDocument;

public interface STDocument extends STObject<ProtoDocument> {

	public <E> STAttribute<E> attribute(int tag);

	public STCollection collection(int tag);

	public STDocument document(int tag);

	public <E> STAttribute<E> getAttribute(int tag);

	public STCollection getCollection(int tag);

	public STDocument getDocument(int tag);

	public String getId();

	public Oid<?> getOid();

	public void setAttribute(int tag, STAttribute<?> attribute);

	public void setCollection(int tag, STCollection collection);

	public void setDocument(int tag, STDocument document);

	public void setOid(Oid<?> oid);
}
