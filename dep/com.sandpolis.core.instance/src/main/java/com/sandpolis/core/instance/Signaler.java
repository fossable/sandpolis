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
package com.sandpolis.core.instance;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides simple event dispatching.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class Signaler<E> {

	private static final Logger log = LoggerFactory.getLogger(Signaler.class);

	/**
	 * Listeners mapped by event type for easy access.
	 */
	private static ConcurrentMap<Object, List<Function<Object, Boolean>>> listeners;

	/**
	 * The thread pool for executing listeners.
	 */
	private static ExecutorService pool;

	public static void init(ExecutorService pool) {
		Signaler.pool = Objects.requireNonNull(pool);
		listeners = new ConcurrentHashMap<>();
	}

	/**
	 * Register a new listener for the given event type.
	 * 
	 * @param type The event type
	 * @param c    The listener
	 * @return The listener reference (same as c)
	 */
	@SuppressWarnings("unchecked")
	public static <E> Object register(Object type, Function<E, Boolean> c) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(c);

		if (!listeners.containsKey(type))
			listeners.put(type, new LinkedList<>());
		listeners.get(type).add((Function<Object, Boolean>) c);
		log.trace("Registered new listener for event: {}", type);

		return c;
	}

	/**
	 * Register a new listener for the given event type.
	 * 
	 * @param type The event type
	 * @param c    The listener
	 * @return The listener reference (not same as c)
	 */
	public static <E> Object register(Object type, Supplier<Boolean> c) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(c);

		return register(type, (E e) -> {
			return c.get();
		});
	}

	/**
	 * Register a new listener for the given event type.
	 * 
	 * @param type The event type
	 * @param c    The listener
	 * @return The listener reference (not same as c)
	 */
	public static <E> Object register(Object type, Consumer<E> c) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(c);

		return register(type, (E e) -> {
			c.accept(e);
			return true;
		});
	}

	/**
	 * Register a new listener for the given event type.
	 * 
	 * @param type The event type
	 * @param c    The listener
	 * @return The listener reference (not same as c)
	 */
	public static Object register(Object type, Runnable c) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(c);

		return register(type, e -> {
			c.run();
			return true;
		});
	}

	/**
	 * Dispatch an event to all registered listeners in an undefined order.
	 * 
	 * @param type The event type
	 * @param e    The event parameter
	 */
	public static <E> void fire(Object type, E e) {
		Objects.requireNonNull(type);
		log.debug("Firing event: {}", type);

		if (listeners.containsKey(type))
			pool.execute(() -> listeners.get(type).removeIf(f -> !f.apply(e)));

	}

	/**
	 * Dispatch an event to all registered listeners without an argument.
	 * 
	 * @param type The event type
	 */
	public static void fire(Object type) {
		fire(type, null);
	}

	/**
	 * Remove a listener registered with the given event type.
	 * 
	 * @param type The event type
	 * @param c    The listener to remove
	 */
	public static void remove(Object type, Object c) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(c);

		if (listeners.containsKey(type))
			listeners.get(type).removeIf(f -> f == c);
	}

	/**
	 * Remove the given listener from all event types.
	 * 
	 * @param c The listener to remove
	 */
	public static void remove(Object c) {
		Objects.requireNonNull(c);

		listeners.values().stream().forEach(list -> list.removeIf(f -> f == c));
	}

	private Signaler() {
	}
}
