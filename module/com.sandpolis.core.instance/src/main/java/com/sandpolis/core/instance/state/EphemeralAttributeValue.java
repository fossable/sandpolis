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
package com.sandpolis.core.instance.state;

/**
 *
 * @param <T> The type of the value
 */
public class EphemeralAttributeValue<T> implements STAttributeValue<T> {

	/**
	 * A epoch timestamp associated with the value.
	 */
	private final long timestamp;

	/**
	 * The ephemeral value.
	 */
	private final T value;

	public EphemeralAttributeValue(T value) {
		this(value, System.currentTimeMillis());
	}

	public EphemeralAttributeValue(T value, long timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}

	@Override
	public T get() {
		return value;
	}

	@Override
	public long timestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		if (value != null)
			return value.toString();
		return null;
	}
}
