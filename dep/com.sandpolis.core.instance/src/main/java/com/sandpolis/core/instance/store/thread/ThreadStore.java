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
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store.AutoInitializer;

/**
 * The {@link ThreadStore} manages all of the application's
 * {@link ExecutorService} objects.
 * 
 * @author cilki
 * @since 5.0.0
 */
@AutoInitializer
public final class ThreadStore {

	private static final Logger log = LoggerFactory.getLogger(ThreadStore.class);

	private static Map<String, ExecutorService> map;

	/**
	 * Associate each id in the given list with the given {@link ExecutorService}.
	 * 
	 * @param executor The new {@link ExecutorService}
	 * @param id       The list of IDs
	 */
	public static void register(ExecutorService executor, String... id) {
		Objects.requireNonNull(executor);
		if (map == null)
			map = new HashMap<>();

		for (String s : id)
			map.put(s, executor);
	}

	/**
	 * Get the {@link ExecutorService} corresponding to the given identifier.
	 * 
	 * @param id The identifier
	 * @return A {@link ExecutorService} or {@code null} if the service does not
	 *         exist
	 */
	@SuppressWarnings("unchecked")
	public static <E> E get(String id) {
		if (map == null)
			// Invoke unit test
			autoinit();

		return (E) map.get(Objects.requireNonNull(id));
	}

	// TODO shutdown method

	/**
	 * Automatically initialize the store with default {@link ExecutorService}s for
	 * easier unit testing.
	 */
	private static void autoinit() {
		log.warn("Automatically initializing ThreadStore");
		register(Executors.newSingleThreadExecutor(), "signaler", "generator");

		try {
			Class<?> c = Class.forName("io.netty.channel.nio.NioEventLoopGroup");
			Object instance = c.getConstructor(Integer.class).newInstance(8);

			register((ExecutorService) instance, "net.connection.outgoing", "net.message.incoming", "net.exelet");
			register((ExecutorService) instance.getClass().getMethod("next").invoke(instance), "dns");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private ThreadStore() {
	}
}
