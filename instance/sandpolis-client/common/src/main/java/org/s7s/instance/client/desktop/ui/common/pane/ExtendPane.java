//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common.pane;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javafx.animation.Animation.Status;
import javafx.animation.Transition;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * {@link ExtendPane} allows {@link Region}s to be raised over a central primary
 * {@link Node} similar to a {@link BorderPane}.
 *
 * @since 5.0.0
 */
public class ExtendPane extends BorderPane {

	/**
	 * The animation map. If a {@link Side} exists in the map, then the side is
	 * currently extended (any could also be executing an animation).
	 */
	private Map<Side, Transition> animationMap = new HashMap<>(1, 1.0f);

	private final ObjectProperty<Duration> duration = new SimpleObjectProperty<>(Duration.millis(500));

	private final ObjectProperty<Region> regionBottom = new SimpleObjectProperty<>();
	private final ObjectProperty<Region> regionLeft = new SimpleObjectProperty<>();
	private final ObjectProperty<Region> regionRight = new SimpleObjectProperty<>();
	private final ObjectProperty<Region> regionTop = new SimpleObjectProperty<>();

	/**
	 * Construct a new {@link ExtendPane} around the given {@link Node}.
	 *
	 * @param main The central node which will take up the entire container until
	 *             something is extended
	 */
	public ExtendPane(@NamedArg("main") Region main) {
		setCenter(Objects.requireNonNull(main));

		regionTop.addListener((p, o, n) -> {
			if (n == null) {
				drop(Side.TOP);
			} else {
				raise(n, Side.TOP);
			}
		});
		regionBottom.addListener((p, o, n) -> {
			if (n == null) {
				drop(Side.BOTTOM);
			} else {
				raise(n, Side.BOTTOM);
			}
		});
		regionLeft.addListener((p, o, n) -> {
			if (n == null) {
				drop(Side.LEFT);
			} else {
				raise(n, Side.LEFT);
			}
		});
		regionRight.addListener((p, o, n) -> {
			if (n == null) {
				drop(Side.RIGHT);
			} else {
				raise(n, Side.RIGHT);
			}
		});
	}

	/**
	 * Add the given node to the border layout.
	 *
	 * @param side The side
	 * @param node The node to add or {@code null}
	 */
	private void addToLayout(Side side, Region node) {
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
	 * Drop the given side if extended.
	 *
	 * @param side The side to drop
	 */
	private void drop(Side side) {
		Objects.requireNonNull(side);
		if (animationMap.containsKey(side) && !isMoving(side))
			animationMap.get(side).play();
	}

	public ObjectProperty<Duration> durationProperty() {
		return duration;
	}

	/**
	 * Get whether the given side has a running animation.
	 *
	 * @param side The given side
	 * @return Whether the side has a running animation
	 */
	public boolean isMoving(Side side) {
		if (!animationMap.containsKey(side))
			return false;
		return animationMap.get(side).getStatus() == Status.RUNNING;
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
	private boolean raise(Region region, Side side) {
		Objects.requireNonNull(region);
		Objects.requireNonNull(side);

		// TODO
		double size = 180;

		// Bypass animation if possible
		if (duration.get() == Duration.ZERO) {
			switch (side) {
			case TOP:
			case BOTTOM:
				region.setPrefHeight(size);
				break;
			case RIGHT:
			case LEFT:
				region.setPrefWidth(size);
				break;
			}
			addToLayout(side, region);
			return true;
		}

		if (isMoving(side))
			// An animation for this side is in progress
			return false;

		// Use one transition to show the node and reverse it to hide
		Transition show;

		// The region needs to be wrapped in a Pane
		Region wrapped = new Pane(region);

		// The TOP and LEFT cases are handled by moving the wrapper pane onto the stage
		// from just outside of the stage without resizing the wrapper.
		// The BOTTOM and RIGHT cases are handled by simply resizing the wrapper pane.
		switch (side) {
		case TOP:
			region.setPrefHeight(size);
			region.prefWidthProperty().bind(this.widthProperty());
			wrapped.setPrefHeight(0);

			// Make the node invisible to prevent it from appearing before the animation
			// begins. It will become visible on the first frame.
			wrapped.setVisible(false);
			show = new Transition() {

				private boolean first = true;

				{
					setCycleDuration(duration.get());
				}

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
			region.prefHeightProperty().bind(this.heightProperty());
			wrapped.setPrefWidth(0);

			// Make the node invisible to prevent it from appearing before the animation
			// begins. It will become visible on the first frame.
			wrapped.setVisible(false);
			show = new Transition() {

				private boolean first = true;

				{
					setCycleDuration(duration.get());
				}

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
			region.prefWidthProperty().bind(this.widthProperty());
			wrapped.setPrefHeight(0);
			show = new Transition() {

				{
					setCycleDuration(duration.get());
				}

				@Override
				protected void interpolate(double frac) {
					wrapped.setPrefHeight((region.getHeight() == 0 ? 1 : region.getHeight()) * frac);
				}
			};

			break;
		case RIGHT:
			region.prefHeightProperty().bind(this.heightProperty());
			wrapped.setPrefWidth(0);
			show = new Transition() {

				{
					setCycleDuration(duration.get());
				}

				@Override
				protected void interpolate(double frac) {
					wrapped.setPrefWidth((region.getWidth() == 0 ? 1 : region.getWidth()) * frac);
				}
			};
			break;
		default:
			throw new RuntimeException();
		}

		show.onFinishedProperty().set(e -> {
			show.setRate(-show.getRate());
			show.onFinishedProperty().set(f -> {
				animationMap.remove(side);
				addToLayout(side, null);
			});
		});

		if (animationMap.containsKey(side)) {
			// Need to drop the extended side first
			animationMap.get(side).onFinishedProperty().set(e -> {
				animationMap.put(side, show);

				addToLayout(side, wrapped);
				show.play();
			});
			drop(side);
		} else {
			animationMap.put(side, show);

			addToLayout(side, wrapped);
			show.play();
		}

		return true;
	}

	public ObjectProperty<Region> regionBottomProperty() {
		return regionBottom;
	}

	public ObjectProperty<Region> regionLeftProperty() {
		return regionLeft;
	}

	public ObjectProperty<Region> regionRightProperty() {
		return regionRight;
	}

	public ObjectProperty<Region> regionTopProperty() {
		return regionTop;
	}
}
