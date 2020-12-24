//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.store.event;

import com.google.common.eventbus.EventBus;

/**
 * An event for use with an {@link EventBus}.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class Event {
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
