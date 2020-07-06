//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.instance.store;

import java.util.ConcurrentModificationException;
import java.util.concurrent.Semaphore;

/**
 * A superclass that assists with concurrency support for store providers. This
 * class contains a central {@link Semaphore} that tracks the number of
 * available "iteration slots" which are required before threads may iterate the
 * store. Mutator operations must acquire <b>all</b> iterations slots before
 * proceeding to ensure a {@link ConcurrentModificationException} is not thrown.
 * This means that iterations of the store should be done quickly because they
 * block all mutator operations.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class ConcurrentStoreProvider<E> implements StoreProvider<E> {

	/**
	 * The maximum number of concurrent iterators.
	 */
	private static final int CONCURRENCY = 8;

	/**
	 * The iteration semaphore.
	 */
	private final Semaphore iteration = new Semaphore(CONCURRENCY);

	/**
	 * Indicates that a stream is about to begin and requests a new stream slot.
	 */
	protected void beginStream() {
		try {
			iteration.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Indicate that a stream has completed and its resource should be released.
	 */
	protected void endStream() {
		iteration.release();
	}

	/**
	 * Represents the action of a mutator. This interface exists only because the
	 * JDK doesn't have a single method interface with:
	 * {@code void run() throws Exception}.
	 */
	@FunctionalInterface
	public static interface Mutator {
		public void run() throws Exception;
	}

	/**
	 * Mutate the store provider with the given mutator. This method will wait until
	 * all iterators and mutators have completed before running the given operation.
	 *
	 * @param action The mutator lambda
	 */
	protected void mutate(Mutator action) {
		try {
			iteration.acquire(CONCURRENCY);
			action.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			iteration.release(CONCURRENCY);
		}
	}
}
