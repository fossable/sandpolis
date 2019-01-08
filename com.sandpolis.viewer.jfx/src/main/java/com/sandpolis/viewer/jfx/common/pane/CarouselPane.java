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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javafx.animation.Animation.Status;
import javafx.animation.Transition;
import javafx.beans.NamedArg;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A CarouselPane holds multiple panes vertically or horizonally. A direction
 * must be specified as the "forward" transition direction otherwise
 * {@code RIGHT} will be used.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class CarouselPane extends StackPane {

	public enum Side {
		LEFT, RIGHT, UP, DOWN;
	}

	/**
	 * All possible views in this {@code CarouselPane}.
	 */
	private List<Node> views;

	private Side direction;

	/**
	 * The current view index.
	 */
	private int current = 0;

	private abstract class SlideTransition extends Transition {
		protected Node left_up;
		protected Node right_down;

		private double rate;

		public void reset() {
			setRate(rate);
			jumpTo(Duration.ZERO);

			left_up = null;
			right_down = null;
		}

		public void setDuration(Duration duration) {
			setCycleDuration(duration);
			rate = getRate();
		}

		public void setNodes(Node left_up, Node right_down) {
			this.left_up = left_up;
			this.right_down = right_down;
		}

	}

	private final SlideTransition right = new SlideTransition() {

		@Override
		protected void interpolate(double frac) {
			final double position = getWidth() * frac;

			left_up.setTranslateX(-getWidth() + position);
			right_down.setTranslateX(position);
		}
	};

	private final SlideTransition down = new SlideTransition() {

		@Override
		protected void interpolate(double frac) {
			final double position = getHeight() * frac;

			left_up.setTranslateY(-getHeight() + position);
			right_down.setTranslateY(position);
		}
	};

	/**
	 * Create a new {@code CarouselPane} from the given components.
	 * 
	 * @param direction The direction that should be considered "forward" in the
	 *                  transitions
	 * @param duration  The transition time in milliseconds
	 * @param children  Nodes in the carousel
	 */
	public CarouselPane(@NamedArg("direction") String direction, @NamedArg("duration") int duration,
			@NamedArg("children") Node... children) {
		if (direction == null)
			throw new IllegalArgumentException();
		if (children == null || children.length == 0)
			throw new IllegalArgumentException();
		if (duration < 0)
			throw new IllegalArgumentException();

		right.setDuration(Duration.millis(duration));
		down.setDuration(Duration.millis(duration));
		this.views = Arrays.asList(children);
		this.direction = Side.valueOf(direction.toUpperCase());

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/common/pane/CarouselPane.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		fxmlLoader.setClassLoader(getClass().getClassLoader());

		try {
			fxmlLoader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		getChildren().add(views.get(0));
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
	 * Move the view back by 1.
	 */
	public void moveBackward() {
		moveBackward(1);
	}

	/**
	 * Move the view ahead by 1.
	 */
	public void moveForward() {
		moveForward(1);
	}

	/**
	 * Move the view to the pane n spots behind the current pane.
	 * 
	 * @param n The number of pages to jump
	 */
	public void moveBackward(int n) {
		moveTo(current - n);
	}

	/**
	 * Move the view to the pane n spots ahead of the current pane.
	 * 
	 * @param n The number of pages to jump
	 */
	public void moveForward(int n) {
		moveTo(current + n);
	}

	/**
	 * Move the view to the given index.
	 * 
	 * @param n The desired index
	 */
	public void moveTo(int n) {
		if (n < 0 || n >= views.size())
			throw new IllegalArgumentException();
		if (n == current)
			return;
		if (isMoving())
			return;

		Node next = views.get(n);
		Node curr = views.get(current);

		final SlideTransition move;
		switch (direction) {
		case UP:
		case DOWN:
			move = down;
			break;
		case LEFT:
		case RIGHT:
			move = right;
			break;
		default:
			move = null;
			break;
		}

		move.onFinishedProperty().set((ActionEvent actionEvent) -> {
			move.reset();
			getChildren().remove(curr);

			current = n;
		});

		switch (direction) {
		case RIGHT:
		case DOWN:
			move.setNodes(n > current ? next : curr, n > current ? curr : next);
			if (n < current) {
				// Play backwards
				move.setRate(-move.getRate());
				move.jumpTo(Duration.INDEFINITE);
			}
			break;
		case LEFT:
		case UP:
			move.setNodes(n < current ? next : curr, n < current ? curr : next);
			if (n > current) {
				// Play backwards
				move.setRate(-move.getRate());
				move.jumpTo(Duration.INDEFINITE);
			}
			break;
		}

		getChildren().add(next);
		move.play();
	}

	/**
	 * Get the pane's transition status.
	 * 
	 * @return Whether the pane is currently transitioning
	 */
	public boolean isMoving() {
		return right.getStatus() == Status.RUNNING || down.getStatus() == Status.RUNNING;
	}
}
