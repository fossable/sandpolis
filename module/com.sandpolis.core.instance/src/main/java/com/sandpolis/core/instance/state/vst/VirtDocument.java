package com.sandpolis.core.instance.state.vst;

import com.sandpolis.core.instance.state.st.STDocument;

public abstract class VirtDocument implements VirtObject {

	protected STDocument document;

	public VirtDocument(STDocument document) {
		this.document = document;
	}

	public abstract long tag();
}
