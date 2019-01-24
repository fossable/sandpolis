/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.common.controller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.common.eventbus.EventBus;
import com.sandpolis.viewer.jfx.common.event.Event;
import com.sandpolis.viewer.jfx.common.event.ParameterizedEvent;

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
		} else
			Arrays.stream(children).forEach(bus::register);
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
	 * @param c The event constructor
	 */
	protected void post(Supplier<? extends Event> c) {
		bus.post(c.get());
	}

	/**
	 * Post the given {@link ParameterizedEvent} to the {@link EventBus}.
	 * 
	 * @param c         The event constructor
	 * @param parameter The event parameter
	 */
	protected <E> void post(Supplier<? extends ParameterizedEvent<E>> c, E parameter) {
		ParameterizedEvent<E> event = c.get();

		try {
			// Set the parameter with reflection
			Field field = event.getClass().getSuperclass().getDeclaredField("object");
			field.setAccessible(true);
			field.set(event, parameter);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		bus.post(event);
	}

}