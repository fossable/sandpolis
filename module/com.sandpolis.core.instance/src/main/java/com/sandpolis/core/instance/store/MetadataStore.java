package com.sandpolis.core.instance.store;

/**
 * A store that tracks metadata such as the total number of times the store has
 * been initialized, usage statistics, and any other domain-specific information
 * that may be useful.
 *
 * @param <E> The metadata type
 */
public interface MetadataStore<E extends StoreMetadata> {

	/**
	 * Get a handle on the store's metadata which is updated in real-time.
	 * 
	 * @return The metadata object
	 */
	public E getMetadata();
}
