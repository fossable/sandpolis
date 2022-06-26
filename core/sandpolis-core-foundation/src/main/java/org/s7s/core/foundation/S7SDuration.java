//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public record S7SDuration(Duration duration) {

	/**
	 * The default units (in descending order) in which to format durations.
	 */
	private static final ChronoUnit[] DEFAULT_DURATION_UNITS = new ChronoUnit[] { ChronoUnit.DAYS, ChronoUnit.HOURS,
			ChronoUnit.MINUTES, ChronoUnit.SECONDS };

	public static S7SDuration of(Duration duration) {
		return new S7SDuration(duration);
	}

	/**
	 * Format the given time duration in English according to the given units.
	 *
	 * @param units The units
	 * @return A formatted string
	 */
	public String format(ChronoUnit... units) {
		if (units.length == 0) {
			// Default units
			units = DEFAULT_DURATION_UNITS;
		} else {
			// Sort input units
			Arrays.sort(units, (u1, u2) -> u2.compareTo(u1));
		}

		// Shortcut for nil duration
		if (duration.isZero()) {
			return "0 " + units[units.length - 1];
		}

		var _duration = duration;

		var buffer = new StringBuffer();

		// Compute the number of times each unit fits into the value
		long[] u = new long[units.length];
		for (int i = 0; i < units.length; i++) {
			if (_duration.compareTo(units[i].getDuration()) >= 0) {
				u[i] = _duration.dividedBy(units[i].getDuration());
				_duration = _duration.minus(units[i].getDuration().multipliedBy(u[i]));
			}
		}

		// Assemble the output text
		for (int i = 0; i < units.length; i++) {

			// Append value and unit if greater than 0
			if (u[i] == 1) {
				buffer.append(u[i]);
				buffer.append(' ');
				// TODO singular unit
				buffer.append(units[i]);
			} else if (u[i] > 1) {
				buffer.append(u[i]);
				buffer.append(' ');
				buffer.append(units[i]);
			} else {
				continue;
			}

			// Append a comma if there's more to come
			for (int j = i + 1; j < units.length; j++) {
				if (u[j] > 0) {
					buffer.append(", ");
					break;
				}
			}
		}

		return buffer.toString();
	}
}
