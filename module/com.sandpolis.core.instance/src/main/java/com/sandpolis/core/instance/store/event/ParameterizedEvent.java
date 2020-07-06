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
package com.sandpolis.core.instance.store.event;

/**
 * An event that contains an {@link Object} parameter.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class ParameterizedEvent<E> extends Event {

	private E parameter;

	protected ParameterizedEvent(E parameter) {
		this.parameter = parameter;
	}

	/**
	 * Get the event's parameter.
	 *
	 * @return The event parameter
	 */
	public E get() {
		return parameter;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + parameter + ")";
	}
}
