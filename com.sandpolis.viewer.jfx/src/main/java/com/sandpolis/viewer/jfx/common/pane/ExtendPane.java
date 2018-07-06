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

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.NamedArg;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * The {@link ExtendPane} allows secondary {@link Node}s to be raised over a
 * central primary {@link Node}.
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
	 * A map of in-progress animations that are bringing a node into view.
	 */
	private Map<ExtendSide, Animation> show_map = new HashMap<>();

	/**
	 * A map of pending animations that will bring a node out of view.
	 */
	private Map<ExtendSide, Animation> hide_map = new HashMap<>();

	/**
	 * A map of currently shown nodes.
	 */
	private Map<ExtendSide, Node> node_map = new HashMap<>();

	/**
	 * Get the {@link Node} that is currently extended on the given side.
	 * 
	 * @param side
	 *            An {@link ExtendSide}
	 * @return The {@link Node} extended on the given side
	 */
	public Node get(ExtendSide side) {
		return node_map.get(side);
	}

	/**
	 * Drop the given side.
	 * 
	 * @param side
	 *            The side to drop
	 */
	public void drop(ExtendSide side) {
		if (side == null)
			throw new IllegalArgumentException();
		if (!hide_map.containsKey(side))
			throw new IllegalStateException("The side: " + side + " is not extended");

		hide_map.remove(side).play();
	}

	/**
	 * Drop all extended sides simultaneously.
	 */
	public void drop() {
		for (Animation hide : hide_map.values())
			hide.play();

		hide_map.clear();
	}

	/**
	 * Move a new {@link Pane} in from the given side. If the side is already
	 * extended, it will be dropped first.
	 * 
	 * @param pane
	 *            The pane to extend
	 * @param side
	 *            The side to extend from
	 * @param duration
	 *            The duration of the transition in milliseconds
	 * @param absWidth
	 *            The relative width of the extended pane as a percentage
	 */
	public void raise(Pane pane, ExtendSide side, int duration, float absWidth) {
		if (pane == null)
			throw new IllegalArgumentException();
		if (side == null)
			throw new IllegalArgumentException();
		if (duration < 0)
			throw new IllegalArgumentException();
		if (absWidth < 0 || absWidth > 1.0)
			throw new IllegalArgumentException();

		// TODO relative using property bindings
	}

	/**
	 * Move a new {@link Pane} in from the given side. If the side is already
	 * extended, it will be dropped first.
	 * 
	 * @param pane
	 *            The pane to extend
	 * @param side
	 *            The side to extend from
	 * @param duration
	 *            The duration of the transition in milliseconds
	 * @param absWidth
	 *            The absolute width of the extended pane in pixels
	 */
	public void raise(Pane pane, ExtendSide side, int duration, int absWidth) {
		if (pane == null)
			throw new IllegalArgumentException();
		if (side == null)
			throw new IllegalArgumentException();
		if (duration < 0)
			throw new IllegalArgumentException();
		if (absWidth < 0)
			throw new IllegalArgumentException();

		if (show_map.containsKey(side)) {
			// An animation for this side is in progress
			return;
		}

		Animation hide = new Transition() {
			{
				setCycleDuration(Duration.millis(duration));
			}

			protected void interpolate(double frac) {
				final double curWidth = absWidth * (1.0 - frac);
				pane.setPrefWidth(curWidth);
				pane.setTranslateX(-absWidth + curWidth);
			}
		};

		Animation show = new Transition() {
			{
				setCycleDuration(Duration.millis(duration));
			}

			protected void interpolate(double frac) {
				final double curWidth = absWidth * frac;
				pane.setPrefWidth(curWidth);
				pane.setTranslateX(-absWidth + curWidth);
			}
		};
		show.onFinishedProperty().set((ActionEvent event) -> {
			show_map.remove(side);
			hide_map.put(side, hide);
		});
		hide.onFinishedProperty().set((ActionEvent event) -> {
			setSide(side, null);
		});

		if (hide_map.containsKey(side)) {
			// Need to drop the side first
			hide_map.get(side).onFinishedProperty().set((ActionEvent event) -> {
				setSide(side, pane);

				show_map.put(side, show);
				show.play();
			});
			drop(side);
		} else {
			setSide(side, pane);

			show_map.put(side, show);
			show.play();

		}
	}

	private void setSide(ExtendSide side, Node node) {
		node_map.put(side, node);
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

	/**
	 * Construct a new {@link ExtendPane} around the given {@link Node}.
	 * 
	 * @param main
	 *            The primary node which will take up the entire container until
	 *            something is extended
	 */
	public ExtendPane(@NamedArg("main") Node main) {
		setCenter(main);
	}

}
