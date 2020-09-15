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
 * {@link EphemeralAttribute} allows attributes to be persistent and optionally
 * saves the history of the attribute's value.
 *
 * @param <T> The type of the attribute's value
 * @since 7.0.0
 */
public class EphemeralAttribute<T> extends AbstractSTAttribute<T> implements STAttribute<T> {

	public EphemeralAttribute(STDocument parent) {
		this.parent = parent;
	}

	@Override
	protected STAttributeValue<T> newValue(T value) {
		return new EphemeralAttributeValue<>(value);
	}

	@Override
	protected STAttributeValue<T> newValue(T value, long timestamp) {
		return new EphemeralAttributeValue<>(value, timestamp);
	}
}
