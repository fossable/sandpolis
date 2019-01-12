/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.common.pane;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javafx.animation.Animation.Status;
import javafx.animation.Transition;
import javafx.beans.NamedArg;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * An {@link ExtendPane} allows {@link Region}s to be raised over a central
 * primary {@link Node} similar to a {@link BorderPane}. This class is not
 * thread-safe.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ExtendPane extends BorderPane {

	/**
	 * Represents a side in an {@link ExtendPane}.
	 */
	public enum ExtendSide {
		LEFT, RIGHT, TOP, BOTTOM;
	}

	/**
	 * A wrapper for {@link Transition}.
	 */
	private abstract class ExtendTransition extends Transition {

		private Region region;

		public Region getRegion() {
			return region;
		}

		public ExtendTransition(Region region, double duration) {
			this.region = Objects.requireNonNull(region);
			setCycleDuration(Duration.millis(duration));
		}
	}

	/**
	 * The animation map. If an {@link ExtendSide} exists in the map, then the side
	 * is currently extended (any could also be executing an animation).
	 */
	private Map<ExtendSide, ExtendTransition> map = new HashMap<>(1, 1.0f);

	/**
	 * Construct a new {@link ExtendPane} around the given {@link Node}.
	 * 
	 * @param main The central node which will take up the entire container until
	 *             something is extended
	 */
	public ExtendPane(@NamedArg("main") Node main) {
		setCenter(Objects.requireNonNull(main));
	}

	/**
	 * Get the {@link Region} that is currently extended on the given side.
	 * 
	 * @param side An {@link ExtendSide}
	 * @return The {@link Region} extended on the given side
	 */
	public Region get(ExtendSide side) {
		if (map.containsKey(side))
			return map.get(side).getRegion();
		return null;
	}

	/**
	 * Get whether the given side has a running animation.
	 * 
	 * @param side The given side
	 * @return Whether the side has a running animation
	 */
	public boolean isMoving(ExtendSide side) {
		if (!map.containsKey(side))
			return false;
		return map.get(side).getStatus() == Status.RUNNING;
	}

	/**
	 * Drop the given side if extended.
	 * 
	 * @param side The side to drop
	 */
	public void drop(ExtendSide side) {
		Objects.requireNonNull(side);
		if (map.containsKey(side) && !isMoving(side))
			map.get(side).play();
	}

	/**
	 * Drop all extended sides simultaneously.
	 */
	public void drop() {
		for (ExtendSide side : ExtendSide.values())
			drop(side);
	}

	/**
	 * Move a new {@link Region} in from the given side. If the side is already
	 * extended, it will be dropped first.
	 * 
	 * @param region   The pane to extend
	 * @param side     The side to extend from
	 * @param duration The duration of the transition in milliseconds
	 * @param size     The relative length of the extended pane as a percentage
	 *                 (along the transition axis)
	 * @return Whether the transition was scheduled
	 */
	public boolean raise(Region region, ExtendSide side, int duration, float size) {
		Objects.requireNonNull(region);
		Objects.requireNonNull(side);
		if (duration < 0)
			throw new IllegalArgumentException();
		if (size < 0 || size > 1.0)
			throw new IllegalArgumentException();

		// TODO relative using property bindings
		throw new UnsupportedOperationException();
	}

	/**
	 * Move a new {@link Region} in from the given side. If the side is already
	 * extended, it will be dropped first.
	 * 
	 * @param region   The region to extend
	 * @param side     The side to extend from
	 * @param duration The duration of the transition in milliseconds
	 * @param size     The absolute length of the extended pane in pixels (along the
	 *                 transition axis)
	 * @return Whether the transition was scheduled
	 */
	public boolean raise(Region region, ExtendSide side, int duration, int size) {
		Objects.requireNonNull(region);
		Objects.requireNonNull(side);
		if (duration < 0)
			throw new IllegalArgumentException();
		if (size < 0)
			throw new IllegalArgumentException();

		if (isMoving(side)) {
			// An animation for this side is in progress
			return false;
		}

		// Use one transition to show the node and reverse it to hide
		ExtendTransition show;

		// The region needs to be wrapped in a Pane
		Region wrapped = new Pane(region);

		switch (side) {
		case TOP:
			region.setPrefHeight(size);
			wrapped.setPrefHeight(0);

			// Make the node invisible to prevent it from appearing before the animation
			// begins. It should become visible on the first frame.
			wrapped.setVisible(false);
			show = new ExtendTransition(region, duration) {

				private boolean first = true;

				@Override
				protected void interpolate(double frac) {
					if (first) {
						wrapped.setVisible(true);
						first = false;
					}

					final double timeline = size * frac;
					region.setPrefHeight(timeline);
					region.setTranslateY(-size + timeline);
				}
			};
			break;
		case LEFT:
			region.setPrefWidth(size);
			wrapped.setPrefWidth(0);

			// Make the node invisible to prevent it from appearing before the animation
			// begins. It should become visible on the first frame.
			wrapped.setVisible(false);
			show = new ExtendTransition(region, duration) {

				private boolean first = true;

				@Override
				protected void interpolate(double frac) {
					if (first) {
						wrapped.setVisible(true);
						first = false;
					}

					final double timeline = size * frac;
					wrapped.setPrefWidth(timeline);
					wrapped.setTranslateX(-size + timeline);

				}
			};
			break;
		case BOTTOM:
			region.setPrefHeight(size);
			wrapped.setPrefHeight(0);
			show = new ExtendTransition(region, duration) {

				@Override
				protected void interpolate(double frac) {
					wrapped.setPrefHeight(size * frac);
				}
			};
			break;
		case RIGHT:
			region.setPrefWidth(size);
			wrapped.setPrefWidth(0);
			show = new ExtendTransition(region, duration) {

				@Override
				protected void interpolate(double frac) {
					wrapped.setPrefWidth(size * frac);
				}
			};
			break;
		default:
			throw new RuntimeException();
		}

		show.onFinishedProperty().set(e -> {
			show.setRate(-show.getRate());
			show.onFinishedProperty().set(f -> {
				map.remove(side);
				addToLayout(side, null);
			});
		});

		if (map.containsKey(side)) {
			// Need to drop the extended side first
			map.get(side).onFinishedProperty().set(e -> {
				map.put(side, show);

				addToLayout(side, wrapped);
				show.play();
			});
			drop(side);
		} else {
			map.put(side, show);

			addToLayout(side, wrapped);
			show.play();
		}

		return true;
	}

	/**
	 * Add the given node to the border layout.
	 * 
	 * @param side The side
	 * @param node The node to add or {@code null}
	 */
	private void addToLayout(ExtendSide side, Region node) {
		switch (side) {
		case BOTTOM:
			setBottom(node);
			return;
		case LEFT:
			setLeft(node);
			return;
		case RIGHT:
			setRight(node);
			return;
		case TOP:
			setTop(node);
			return;
		}
	}
}
