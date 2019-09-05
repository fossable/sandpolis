/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.profile.store;

import java.util.Objects;
import java.util.Optional;

import com.sandpolis.core.attribute.AttributeDomainKey;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.storage.StoreProvider;

/**
 * A store for the instance's {@link AttributeDomainKey}s.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class DomainStore {

	private static StoreProvider<AttributeDomainKey> provider;

	static {
		init(new MemoryListStoreProvider<>(AttributeDomainKey.class));
	}

	public static void init(StoreProvider<AttributeDomainKey> provider) {
		DomainStore.provider = Objects.requireNonNull(provider);
	}

	/**
	 * Get a root {@link AttributeDomainKey} from the store.
	 * 
	 * @param id The {@link AttributeDomainKey}'s domain
	 * @return The {@link AttributeDomainKey} for the instance
	 */
	public static AttributeDomainKey get(String id) {
		if (id == null)
			id = "";

		Optional<AttributeDomainKey> key = provider.get(id);
		if (key.isPresent())
			return key.get();

		provider.add(new AttributeDomainKey(id));
		return provider.get(id).get();
	}

	private DomainStore() {
	}
}
