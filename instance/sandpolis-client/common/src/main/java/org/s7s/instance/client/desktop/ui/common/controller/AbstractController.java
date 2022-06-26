//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.eventbus.EventBus;

/**
 * A superclass for controllers that need access to an {@link EventBus}.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class AbstractController {

	private List<AbstractController> children;

	/**
	 * The {@link EventBus} for all controllers in the hierarchy.
	 */
	protected EventBus bus;

	/**
	 * Register the child controllers of {@code this} so they receive the same
	 * {@link EventBus}. This may be called in {@code initialize()}.
	 *
	 * @param children A list of subcontrollers
	 */
	public void register(AbstractController... children) {
		if (children.length == 0)
			throw new IllegalArgumentException();

		if (bus == null) {
			if (this.children == null)
				this.children = new ArrayList<>();
			Arrays.stream(children).forEach(this.children::add);
		} else {
			Arrays.stream(children).forEach(bus::register);
		}
	}

	/**
	 * Set the {@link EventBus} for this controller and all children. This method
	 * can be called once.
	 *
	 * @param bus The new {@link EventBus}
	 */
	public void setBus(EventBus bus) {
		if (this.bus != null)
			throw new IllegalStateException();

		this.bus = Objects.requireNonNull(bus);
		bus.register(this);

		if (children != null) {
			children.forEach(controller -> {
				controller.setBus(bus);
				bus.register(controller);
			});
			children = null;
		}
	}

	/**
	 * Get the {@link EventBus} for the controller hierarchy.
	 *
	 * @return The {@link EventBus} or {@code null} if {@link #setBus(EventBus)} has
	 *         not been called
	 */
	public EventBus getBus() {
		return bus;
	}

	/**
	 * Post the given event to the {@link EventBus}.
	 *
	 * @param event The event to post
	 */
	protected void post(Object event) {
		bus.post(event);
	}
}
