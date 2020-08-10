package com.sandpolis.core.instance.store.provider;

import java.util.function.Function;

import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.state.StateObject;

public interface StoreProviderFactory {

	public <E extends StateObject> StoreProvider<E> supply(Class<E> type, Function<Document, E> constructor);

}
