/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.instance.store.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStoreConfig;

/**
 * The {@link ThreadStore} manages all of the application's
 * {@link ExecutorService} objects.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class ThreadStore extends MapStore<String, ExecutorService, ThreadStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ThreadStore.class);

	/**
	 * Get the {@link ExecutorService} corresponding to the given identifier.
	 * 
	 * @param id The identifier
	 * @return A {@link ExecutorService} or {@code null} if the service does not
	 *         exist
	 */
	@SuppressWarnings("unchecked")
	public <E extends ExecutorService> E get(String id) {
		return (E) map.get(Objects.requireNonNull(id));
	}

	@Override
	public void close() throws Exception {
		log.debug("Closing ThreadStore (provider: " + map + ")");
		log.debug("Shutting down {} thread pools", map.size());
		map.values().forEach(service -> service.shutdownNow());
		map = null;
	}

	public static final ThreadStore ThreadStore = new ThreadStore();

	@Override
	public void init(Consumer<ThreadStoreConfig> c) {

	}

	public static final class ThreadStoreConfig {

		private Map<String, ExecutorService> map;

		/**
		 * Associate each id in the given list with the given {@link ExecutorService}.
		 * 
		 * @param executor The new {@link ExecutorService}
		 * @param id       The list of IDs
		 */
		public void register(ExecutorService executor, String... id) {
			Objects.requireNonNull(executor);

			for (String s : id)
				map.put(s, executor);
		}
	}
}
