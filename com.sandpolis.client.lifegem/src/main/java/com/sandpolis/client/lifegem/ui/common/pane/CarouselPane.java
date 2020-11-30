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
package com.sandpolis.client.lifegem.ui.common.pane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.animation.Animation.Status;
import javafx.animation.Transition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * {@link CarouselPane} animates vertical or horizontal transitions between
 * views. The {@link #direction} property determines which direction is
 * considered to be "forward".
 *
 * @since 5.0.0
 */
public class CarouselPane extends StackPane {

	private abstract class SlideTransition extends Transition {
		protected Node left_up;
		private double rate;

		protected Node right_down;

		{
			setCycleDuration(duration.get());
			rate = getRate();

			duration.addListener((p, o, n) -> {
				setCycleDuration(n);
				rate = getRate();
			});
		}

		public void reset() {
			setRate(rate);
			jumpTo(Duration.ZERO);

			left_up = null;
			right_down = null;
		}

		public void setNodes(Node left_up, Node right_down) {
			this.left_up = left_up;
			this.right_down = right_down;
		}

	}

	/**
	 * The index of the current view.
	 */
	private final IntegerProperty current = new SimpleIntegerProperty(0);

	/**
	 * The direction that is considered "forward".
	 */
	private final ObjectProperty<Side> direction = new SimpleObjectProperty<>(Side.LEFT);

	/**
	 * The duration of each transition.
	 */
	private final ObjectProperty<Duration> duration = new SimpleObjectProperty<>(Duration.millis(700));

	private final SlideTransition transition_down = new SlideTransition() {

		@Override
		protected void interpolate(double frac) {
			final double position = getHeight() * frac;

			left_up.setTranslateY(-getHeight() + position);
			right_down.setTranslateY(position);
		}
	};

	private final SlideTransition transition_right = new SlideTransition() {

		@Override
		protected void interpolate(double frac) {
			final double position = getWidth() * frac;

			left_up.setTranslateX(-getWidth() + position);
			right_down.setTranslateX(position);
		}
	};

	/**
	 * All possible views in this {@code CarouselPane}.
	 */
	private final List<Node> views = new ArrayList<>();

	/**
	 * Create a new {@code CarouselPane} from the given components.
	 *
	 * @param direction The direction that should be considered "forward" in the
	 *                  transitions
	 * @param duration  The transition time in milliseconds
	 * @param children  Nodes in the carousel
	 */
	public CarouselPane(Node first) {
		views.add(first);
		getChildren().add(views.get(0));

		current.addListener((p, o, n) -> {
			int currentView = (Integer) o;
			int newView = (Integer) n;

			if (newView < 0 || newView >= views.size())
				throw new IllegalArgumentException();
			if (isMoving())
				// TODO Either schedule the transition or return failure
				return;

			Node next = views.get(newView);
			Node curr = views.get(currentView);

			final SlideTransition move;
			switch (direction.get()) {
			case TOP:
			case BOTTOM:
				move = transition_down;
				break;
			case LEFT:
			case RIGHT:
				move = transition_right;
				break;
			default:
				move = null;
				break;
			}

			move.onFinishedProperty().set((ActionEvent actionEvent) -> {
				move.reset();
				getChildren().remove(curr);
			});

			switch (direction.get()) {
			case RIGHT:
			case BOTTOM:
				move.setNodes(newView > currentView ? next : curr, newView > currentView ? curr : next);
				if (newView < currentView) {
					// Play backwards
					move.setRate(-move.getRate());
					move.jumpTo(Duration.INDEFINITE);
				}
				break;
			case LEFT:
			case TOP:
				move.setNodes(newView < currentView ? next : curr, newView < currentView ? curr : next);
				if (newView > currentView) {
					// Play backwards
					move.setRate(-move.getRate());
					move.jumpTo(Duration.INDEFINITE);
				}
				break;
			}

			getChildren().add(next);
			move.play();
		});
	}

	/**
	 * Add a node to the end of the carousel. This method does not move the view.
	 *
	 * @param node The node to add
	 */
	public void add(Node node) {
		views.add(node);
	}

	/**
	 * Add a node to the end of the carousel. This method does not move the view.
	 *
	 * @param name The node's name
	 * @param node The node to add
	 */
	public void add(String name, Node node) {
		node.setUserData(name);
		add(node);
	}

	public ObjectProperty<Side> directionProperty() {
		return direction;
	}

	public ObjectProperty<Duration> durationProperty() {
		return duration;
	}

	/**
	 * Get the pane's transition status.
	 *
	 * @return Whether the pane is currently transitioning
	 */
	public boolean isMoving() {
		return transition_right.getStatus() == Status.RUNNING || transition_down.getStatus() == Status.RUNNING;
	}

	/**
	 * Move the view back by 1.
	 */
	public void moveBackward() {
		moveBackward(1);
	}

	/**
	 * Move the view to the pane n spots behind the current pane.
	 *
	 * @param n The number of pages to jump
	 */
	public void moveBackward(int n) {
		current.set(current.get() - n);
	}

	/**
	 * Move the view ahead by 1.
	 */
	public void moveForward() {
		moveForward(1);
	}

	/**
	 * Move the view to the pane n spots ahead of the current pane.
	 *
	 * @param n The number of pages to jump
	 */
	public void moveForward(int n) {
		current.set(current.get() + n);
	}

	/**
	 * Move the view to the page associated with the given name (which comes from a
	 * node's {@code userData}).
	 *
	 * @param name The page's name
	 */
	public void moveTo(String name) {
		Objects.requireNonNull(name);

		for (int i = 0; i < views.size(); i++) {
			if (name.equals(views.get(i).getUserData())) {
				current.set(i);
				return;
			}
		}

		throw new RuntimeException("Failed to find carousel page: " + name);
	}
}
