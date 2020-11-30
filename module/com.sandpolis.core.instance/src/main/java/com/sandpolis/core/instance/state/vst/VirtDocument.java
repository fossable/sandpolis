package com.sandpolis.core.instance.state.vst;

import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;

public abstract class VirtDocument implements VirtObject {

	protected STDocument document;

	public VirtDocument(STDocument document) {
		this.document = document;
	}

	public Oid oid() {
		return document.oid();
	}

	public abstract String getId();

	public abstract STAttribute<String> id();
}
