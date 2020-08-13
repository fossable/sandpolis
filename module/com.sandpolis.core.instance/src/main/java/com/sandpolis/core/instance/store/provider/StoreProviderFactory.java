package com.sandpolis.core.instance.store.provider;

import java.util.function.Function;

import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.VirtObject;

public interface StoreProviderFactory {

	public <E extends VirtObject> StoreProvider<E> supply(Class<E> type, Function<Document, E> constructor, Oid<?> oid);

}
