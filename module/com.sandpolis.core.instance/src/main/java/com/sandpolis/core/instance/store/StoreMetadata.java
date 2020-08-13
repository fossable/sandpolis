package com.sandpolis.core.instance.store;

/**
 * {@link StoreMetadata} contains information about the store itself.
 */
public interface StoreMetadata {

	/**
	 * Get the number of times the store has been initialized.
	 * 
	 * @return The initialization count
	 */
	public int getInitCount();
}
