package com.sandpolis.core.instance.store.provider;

import java.util.function.Function;

import com.sandpolis.core.instance.data.Document;
import com.sandpolis.core.instance.data.StateObject;

public interface StoreProviderFactory {

	public <E extends StateObject> StoreProvider<E> supply(Class<E> type, Function<Document, E> constructor);

}
