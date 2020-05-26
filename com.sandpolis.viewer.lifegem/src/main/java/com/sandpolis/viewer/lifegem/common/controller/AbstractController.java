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
package com.sandpolis.viewer.lifegem.common.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.eventbus.EventBus;
import com.sandpolis.core.instance.event.Event;
import com.sandpolis.core.instance.event.ParameterizedEvent;

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
	 * Post the given {@link Event} to the {@link EventBus}.
	 *
	 * @param constructor The event constructor
	 */
	protected void post(Supplier<? extends Event> constructor) {
		bus.post(constructor.get());
	}

	/**
	 * Post the given {@link ParameterizedEvent} to the {@link EventBus}.
	 *
	 * @param constructor The event constructor
	 * @param parameter   The event parameter
	 */
	protected <P> void post(Function<P, ? extends ParameterizedEvent<P>> constructor, P parameter) {
		bus.post(constructor.apply(parameter));
	}
}
