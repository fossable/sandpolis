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
package com.sandpolis.client.lifegem.common.label;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import com.sandpolis.core.foundation.util.TextUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * A {@link Label} that displays the amount of time since or until a reference
 * date.
 *
 * @author cilki
 * @since 5.0.0
 */
public class DateLabel extends Label {

	/**
	 * The reference timestamp (milliseconds since 1900).
	 */
	private LongProperty referenceProperty;

	/**
	 * The difference resolution.
	 */
	private ObjectProperty<TimeUnit> resolutionProperty;

	/**
	 * The update loop.
	 */
	private Timeline updateLoop;

	public DateLabel() {
		this(System.currentTimeMillis(), TimeUnit.SECONDS);
	}

	public DateLabel(long reference, TimeUnit resolution) {
		checkArgument(reference > 0);
		checkNotNull(resolution);

		referenceProperty = new SimpleLongProperty(reference);
		referenceProperty.addListener((p, o, n) -> {
			updateLoop.play();
		});

		resolutionProperty = new SimpleObjectProperty<>();
		resolutionProperty.addListener((p, o, n) -> {
			if (updateLoop != null)
				updateLoop.stop();

			Duration refresh;
			switch (resolutionProperty.get()) {
			case DAYS:
				refresh = Duration.hours(24);
				break;
			case HOURS:
				refresh = Duration.hours(1);
				break;
			case MILLISECONDS:
				refresh = Duration.millis(1);
				break;
			case MINUTES:
				refresh = Duration.minutes(1);
				break;
			case SECONDS:
				refresh = Duration.seconds(1);
				break;
			default:
				throw new IllegalArgumentException("Invalid time unit: " + resolutionProperty.get());
			}

			updateLoop = new Timeline(new KeyFrame(refresh, (event) -> {
				long uptime = System.currentTimeMillis() - referenceProperty.get();
				setText(TextUtil.formatDuration(java.time.Duration.ofMillis(uptime)));
			}));
			updateLoop.setCycleCount(Timeline.INDEFINITE);
			updateLoop.play();
		});
		resolutionProperty.set(resolution);
	}

	public ObjectProperty<TimeUnit> resolutionProperty() {
		return resolutionProperty;
	}

	public LongProperty referenceProperty() {
		return referenceProperty;
	}
}
